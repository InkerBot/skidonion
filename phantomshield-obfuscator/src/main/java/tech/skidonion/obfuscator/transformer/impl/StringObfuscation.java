package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.ListIterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class StringObfuscation extends Transformer {

    private final AtomicInteger count = new AtomicInteger(0);

    private int index = 0;


    public StringObfuscation(String name) {
        super(name, false);
    }

    @Override
    public void transform() throws Exception {

        getFilteredClasses().forEach(cw -> {
            ClassNode classNode = cw.getClassNode();
            if (isInterfaceClass(classNode)) return;
            String[] strings = new String[65535];
            int numStrings = 0;
            String decryptorMethodName = generate(index++);
            String decryptedStringsFieldName = generate(index++);
            Random rand = new Random();
            for (MethodNode methods : classNode.methods) {
                ListIterator<AbstractInsnNode> iter = methods.instructions.iterator();
                while (iter.hasNext()) {
                    AbstractInsnNode inst = iter.next();
                    if (inst.getOpcode() == LDC) {
                        LdcInsnNode ldc = (LdcInsnNode) inst;
                        if (ldc.cst instanceof String) {
                            iter.remove();
                            int idx;
                            for (idx = 0; idx < numStrings; idx++) {
                                if (strings[idx] == ldc.cst) {
                                    break;
                                }
                            }
                            if (idx == numStrings) numStrings++;
                            strings[idx] = (String) ldc.cst;
                            iter.add(createNumberNode(idx | (rand.nextInt() & 0xFFFF0000)));
                            iter.add(new InsnNode(I2C));
                            iter.add(new MethodInsnNode(INVOKESTATIC, classNode.name, decryptorMethodName, "(C)Ljava/lang/Object;"));
                            iter.add(new TypeInsnNode(CHECKCAST, Type.getInternalName(String.class)));
                        }
                    }
                }
            }
            if (numStrings > 0) {
                this.count.addAndGet(numStrings);
                classNode.fields.add(new FieldNode(ACC_STATIC, decryptedStringsFieldName, "Ljava/lang/Object;", "", null));
                //TODO:把这个dummy field数量改成可调的

                //TODO:解密时候给dummy field塞入顺序错误的字符串，这样deobf就不能判断哪个是正确的field
                int randomDummy = new Random().nextInt(10);
                for (int i = 0; i < randomDummy; i++) {
                    classNode.fields.add(new FieldNode(ACC_STATIC, generate(index++), "Ljava/lang/Object;", "", null));
                }
                MethodNode methodNode = new MethodNode(ACC_PRIVATE | ACC_STATIC, decryptorMethodName, "(C)Ljava/lang/Object;", null, null);
                methodNode.visitFieldInsn(GETSTATIC, classNode.name, decryptedStringsFieldName, "Ljava/lang/Object;");
                methodNode.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
                methodNode.visitVarInsn(ILOAD, 0);
                methodNode.visitInsn(AALOAD);
                methodNode.visitInsn(ARETURN);
                classNode.methods.add(methodNode);
                MethodNode clinit = getClassInitializer(classNode);

                MethodNode decryptor = generateEmptyVoidMethod(generate(index++));
                classNode.methods.add(decryptor);
                clinit.instructions.insertBefore(clinit.instructions.getFirst(), new MethodInsnNode(INVOKESTATIC, classNode.name, decryptor.name, decryptor.desc));
                generateDecryptor(decryptor, classNode.name, decryptedStringsFieldName, strings, numStrings);
            }
        });
    }

    private boolean isInterfaceClass(ClassNode node) {
        return (node.access & 0x200) != 0;
    }


    private MethodNode getClassInitializer(ClassNode classNode) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals("<clinit>") && method.desc.equals("()V") && (method.access & Opcodes.ACC_STATIC) != 0)
                return method;
        }
        MethodNode methodNode = generateEmptyVoidMethod("<clinit>");
        classNode.methods.add(methodNode);
        return methodNode;
    }

    public String generate(int index) {
        int baseIndex = index / 26;
        int offset = index % 26;

        char newChar = (char) ((offset < 26 ? 'a' : 'A' - 26) + offset);

        if (baseIndex == 0) {
            return String.valueOf(newChar);
        } else {
            return generate(baseIndex - 1) + newChar;
        }
    }

    private MethodNode generateEmptyVoidMethod(String name) {
        MethodNode methodNode = new MethodNode(Opcodes.ACC_STATIC, name, "()V", null, null);
        methodNode.visitInsn(Opcodes.RETURN);
        return methodNode;
    }

    private InsnList getStringInst(String string) {
        InsnList list = new InsnList();
        if (string.getBytes(StandardCharsets.UTF_8).length > 65535) {
            int end;
            list.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(StringBuilder.class)));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "()V"));
            while (string.length() > 0) {
                end = Math.min(string.length(), 65535);
                while (string.substring(0, end).getBytes(StandardCharsets.UTF_8).length > 65535) end--;
                String s = string.substring(0, end);
                string = string.substring(end);
                list.add(new LdcInsnNode(s));
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
            }
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;"));
        } else {
            list.add(new LdcInsnNode(string));
        }
        return list;
    }


    private void generateDecryptor(MethodNode method, String ownerName, String decryptorFieldName, String[] strings, int numStrings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < numStrings; i++) {
            String string = strings[i];
            byte[] b = string.getBytes(StandardCharsets.UTF_8);
            int length = b.length;
            out.write(length & 0xFF);
            out.write((length >> 8) & 0xFF);
            out.write(b, 0, b.length);
        }
        byte[] keyBytes = new byte[8];
        Random rand = new Random();
        rand.nextBytes(keyBytes);
        byte[] data = out.toByteArray();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            byte[] hash = md.digest(data);
            out.write(hash, 0, hash.length);
            data = out.toByteArray();
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            DESKeySpec keySpec = new DESKeySpec(keyBytes);
            Key key = keyFactory.generateSecret(keySpec);
            Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(new byte[8]));
            data = cipher.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | InvalidKeySpecException |
                 IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
            throw new Error(e);
        }

        byte[] swp = new byte[256];
        for (int i = 0; i < swp.length; i++) {
            swp[i] = (byte) i;
        }
        for (int i = 0; i < swp.length; i++) {
            int j;
            do {
                j = rand.nextInt(swp.length);
            } while (i == j);
            byte b = swp[i];
            swp[i] = swp[j];
            swp[j] = b;
        }
        for (int i = 0; i < data.length; i++) {
            data[i] = swp[data[i] & 0xFF];
        }

        InsnList decryptInsts = new InsnList();
        LabelNode realMethodStart = new LabelNode();
        decryptInsts.add(getStringInst(new String(data, StandardCharsets.ISO_8859_1)));
        decryptInsts.add(new LdcInsnNode("ISO_8859_1"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "getBytes", "(Ljava/lang/String;)[B"));
        decryptInsts.add(new VarInsnNode(ASTORE, 0));
        decryptInsts.add(getStringInst(new String(keyBytes, StandardCharsets.ISO_8859_1)));
        decryptInsts.add(new LdcInsnNode("ISO_8859_1"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "getBytes", "(Ljava/lang/String;)[B"));
        decryptInsts.add(new VarInsnNode(ASTORE, 1));

        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, 2));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new InsnNode(ARRAYLENGTH));
        decryptInsts.add(new VarInsnNode(ISTORE, 3));

        // 循环
        LabelNode start = new LabelNode();
        decryptInsts.add(start);
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(generateSwitchCase(swp, rand));
        decryptInsts.add(new InsnNode(I2B));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new IincInsnNode(2, 1));
        decryptInsts.add(new InsnNode(SWAP));
        decryptInsts.add(new InsnNode(BASTORE));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ILOAD, 3));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));


        decryptInsts.add(new LdcInsnNode("DES/CBC/PKCS5Padding"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Cipher.class), "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new InsnNode(ICONST_2));
        decryptInsts.add(new LdcInsnNode("DES"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(SecretKeyFactory.class), "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;"));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(DESKeySpec.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ALOAD, 1));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(DESKeySpec.class), "<init>", "([B)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(SecretKeyFactory.class), "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;"));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(IvParameterSpec.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new IntInsnNode(BIPUSH, 8));
        decryptInsts.add(new IntInsnNode(NEWARRAY, T_BYTE));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(IvParameterSpec.class), "<init>", "([B)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(Cipher.class), "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(Cipher.class), "doFinal", "([B)[B"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ASTORE, 0));
        decryptInsts.add(new InsnNode(ARRAYLENGTH));
        decryptInsts.add(new VarInsnNode(ISTORE, 3));
        decryptInsts.add(new LdcInsnNode("SHA-256"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MessageDigest.class), "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "reset", "()V"));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ILOAD, 3));
        decryptInsts.add(new IntInsnNode(BIPUSH, 32));
        decryptInsts.add(new InsnNode(ISUB));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ISTORE, 2));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "update", "([BII)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "digest", "()[B"));
        decryptInsts.add(new VarInsnNode(ASTORE, 1));

        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, 4));
        decryptInsts.add(new VarInsnNode(ISTORE, 5));
        decryptInsts.add(new VarInsnNode(ISTORE, 6));
        start = new LabelNode();
        decryptInsts.add(start);
        decryptInsts.add(new VarInsnNode(ILOAD, 5));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ILOAD, 4));
        decryptInsts.add(new InsnNode(IADD));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new VarInsnNode(ALOAD, 1));
        decryptInsts.add(new VarInsnNode(ILOAD, 4));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new InsnNode(IXOR));
        decryptInsts.add(new InsnNode(IOR));
        decryptInsts.add(new VarInsnNode(ISTORE, 5));
        decryptInsts.add(new IincInsnNode(4, 1));
        decryptInsts.add(new VarInsnNode(ILOAD, 4));
        decryptInsts.add(new IntInsnNode(BIPUSH, 32));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));
        decryptInsts.add(new VarInsnNode(ILOAD, 5));
        decryptInsts.add(new JumpInsnNode(IFNE, realMethodStart));

        decryptInsts.add(createNumberNode(numStrings));
        decryptInsts.add(new TypeInsnNode(ANEWARRAY, Type.getInternalName(Object.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ASTORE, 1));
        decryptInsts.add(new FieldInsnNode(PUTSTATIC, ownerName, decryptorFieldName, "Ljava/lang/Object;"));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ISTORE, 3));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, 2));
        start = new LabelNode();
        decryptInsts.add(start);

        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new IincInsnNode(2, 1));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new IincInsnNode(2, 1));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(new IntInsnNode(BIPUSH, 8));
        decryptInsts.add(new InsnNode(ISHL));
        decryptInsts.add(new InsnNode(IOR));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ISTORE, 4));
        decryptInsts.add(new IntInsnNode(NEWARRAY, T_BYTE));
        decryptInsts.add(new VarInsnNode(ASTORE, 5));
        decryptInsts.add(new VarInsnNode(ALOAD, 0));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ALOAD, 5));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ILOAD, 4));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(System.class), "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V"));
        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ILOAD, 4));
        decryptInsts.add(new InsnNode(IADD));
        decryptInsts.add(new VarInsnNode(ISTORE, 2));
        decryptInsts.add(new VarInsnNode(ALOAD, 1));
        decryptInsts.add(new VarInsnNode(ILOAD, 6));
        decryptInsts.add(new IincInsnNode(6, 1));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(String.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ALOAD, 5));
        decryptInsts.add(new LdcInsnNode("UTF-8"));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(String.class), "<init>", "([BLjava/lang/String;)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "intern", "()Ljava/lang/String;"));
        decryptInsts.add(new InsnNode(AASTORE));

        decryptInsts.add(new VarInsnNode(ILOAD, 2));
        decryptInsts.add(new VarInsnNode(ILOAD, 3));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));


        decryptInsts.add(realMethodStart);
        method.instructions.insertBefore(method.instructions.getFirst(), decryptInsts);
    }

    private InsnList generateSwitchCase(byte[] swp, Random rand) {
        InsnList insts = new InsnList();
        LabelNode[] idx = new LabelNode[256];
        LabelNode end = new LabelNode();
        for (int i = 0; i < idx.length; i++) {
            int j = 0;
            while ((swp[j] & 0xFF) != i) j++;
            insts.add(idx[i] = new LabelNode());
            insts.add(createNumberNode(j | (rand.nextInt() & 0xFFFFFF00)));
            insts.add(new JumpInsnNode(GOTO, end));
        }
        LabelNode def = new LabelNode();
        insts.add(def);
        insts.add(createNumberNode(rand.nextInt()));
        insts.add(end);
        insts.insertBefore(idx[0], new TableSwitchInsnNode(0, 255, def, idx));
        return insts;
    }

    private AbstractInsnNode createNumberNode(int value) {
        int opcode = getNumberOpcode(value);
        switch (opcode) {
            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                return new InsnNode(opcode);
            default:
                if (value >= -128 && value <= 127) {
                    return new IntInsnNode(Opcodes.BIPUSH, value);
                } else if (value >= -32768 && value <= 32767) {
                    return new IntInsnNode(Opcodes.SIPUSH, value);
                } else {
                    return new LdcInsnNode(value);
                }
        }
    }

    private int getNumberOpcode(int value) {
        switch (value) {
            case -1:
                return Opcodes.ICONST_M1;
            case 0:
                return Opcodes.ICONST_0;
            case 1:
                return Opcodes.ICONST_1;
            case 2:
                return Opcodes.ICONST_2;
            case 3:
                return Opcodes.ICONST_3;
            case 4:
                return Opcodes.ICONST_4;
            case 5:
                return Opcodes.ICONST_5;
            default:
                if (value >= -128 && value <= 127) {
                    return Opcodes.BIPUSH;
                } else if (value >= -32768 && value <= 32767) {
                    return Opcodes.SIPUSH;
                } else {
                    return Opcodes.LDC;
                }
        }
    }

    @Override
    public void preprocess() throws Exception {

    }
    @Override
    public String annotation() {
        return null;
    }
}
