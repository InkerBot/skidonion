package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;

import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class StringEncryption extends Transformer {
    private final AtomicInteger count = new AtomicInteger(0);

    public StringEncryption(String name) {
        super(name, false);
    }

    @Override
    public void transform() throws Exception {
        long current = System.currentTimeMillis();
        getFilteredClasses().forEach(cw -> {
            removeAnnotation(cw);
            if (cw.getAccess().isInterface()) return;
            List<String> strings = new ArrayList<>();
            List<FieldNode> dummys = new ArrayList<>();
            String decryptorMethodName = cw.generateRandomMethodName();
            String decryptedStringsFieldName = cw.generateRandomFieldName();
            cw.getMethods().stream().filter(this::match).forEach(method -> {
                removeAnnotation(method);
                ListIterator<AbstractInsnNode> iter = method.getInstructions().iterator();
                while (iter.hasNext()) {
                    AbstractInsnNode inst = iter.next();
                    if (ASMUtils.isStringInsn(inst)) {
                        String value = ASMUtils.getStringFromInsn(inst);
                        iter.remove();
                        int index = strings.size();
                        strings.add(value);
                        iter.add(ASMUtils.getNumberInsn(index | (RandomUtils.getRandomInt() & 0xFFFF0000)));
                        iter.add(new InsnNode(I2C));
                        iter.add(new MethodInsnNode(INVOKESTATIC, cw.getName(), decryptorMethodName, "(C)Ljava/lang/Object;"));
                        iter.add(new TypeInsnNode(CHECKCAST, Type.getInternalName(String.class)));
                    }
                }
            });
            if (!strings.isEmpty()) {
                if (strings.size() > 0xFFFF)
                    throw new RuntimeException("String Constant Pool is bigger than maximum pool size??");
                this.count.addAndGet(strings.size());
                cw.addField(new FieldNode(ACC_STATIC, decryptedStringsFieldName, "Ljava/lang/Object;", "", null));

                if (strings.size() > 1) { // 只有一个字符串的再多dummy field也没用
                    for (int i = 0; i < Math.min(7, strings.size() - 1); i++) { // 均分 最大7个dummy field
                        final FieldNode fieldNode = new FieldNode(ACC_STATIC, cw.generateRandomFieldName(), "Ljava/lang/Object;", "", null);
                        cw.addField(fieldNode);
                        dummys.add(fieldNode);
                    }
                }
                // 制作缺陷
                MethodNode methodNode = new MethodNode(ACC_PRIVATE | ACC_STATIC, decryptorMethodName, "(C)Ljava/lang/Object;", null, null);
                methodNode.visitFieldInsn(GETSTATIC, cw.getName(), decryptedStringsFieldName, "Ljava/lang/Object;");
                methodNode.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
                methodNode.visitVarInsn(ILOAD, 0);
                methodNode.visitInsn(AALOAD);
                methodNode.visitInsn(ARETURN);
                methodNode.visibleAnnotations = new ArrayList<>();
                methodNode.visibleAnnotations.add(new AnnotationNode("Ltech/skidonion/obfuscator/annotation/NativeObfuacation;"));
                cw.addMethod(methodNode);
                MethodNode clinit = cw.getOrCreateClinit();

                generateDecryptor(clinit, cw.getName(), decryptedStringsFieldName, strings, dummys);
            }
        });
        INFO("Encrypted {} strings... [{}ms]", count.get(), System.currentTimeMillis() - current);
    }

    private void generateDecryptor(MethodNode method, String ownerName, String decryptedStringsFieldName, List<String> strings, List<FieldNode> dummys) {
        final int startIndex = method.maxLocals + 1;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        final List<String> shuffled = shuffleString(strings);
        for (String string : shuffled) {
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
        decryptInsts.add(ASMUtils.getStringInst(new String(data, StandardCharsets.ISO_8859_1)));
        decryptInsts.add(new LdcInsnNode("ISO_8859_1"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "getBytes", "(Ljava/lang/String;)[B"));
        decryptInsts.add(new VarInsnNode(ASTORE, startIndex));
        decryptInsts.add(ASMUtils.getStringInst(new String(keyBytes, StandardCharsets.ISO_8859_1)));
        decryptInsts.add(new LdcInsnNode("ISO_8859_1"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "getBytes", "(Ljava/lang/String;)[B"));
        decryptInsts.add(new VarInsnNode(ASTORE,  startIndex + 1));

        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new InsnNode(ARRAYLENGTH));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 3));

        // 循环
        LabelNode start = new LabelNode();
        decryptInsts.add(start);
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(generateSwitchCase(swp, rand));
        decryptInsts.add(new InsnNode(I2B));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new IincInsnNode(startIndex + 2, 1));
        decryptInsts.add(new InsnNode(SWAP));
        decryptInsts.add(new InsnNode(BASTORE));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 3));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));


        decryptInsts.add(new LdcInsnNode("DES/CBC/PKCS5Padding"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(Cipher.class), "getInstance", "(Ljava/lang/String;)Ljavax/crypto/Cipher;"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new InsnNode(ICONST_2));
        decryptInsts.add(new LdcInsnNode("DES"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(SecretKeyFactory.class), "getInstance", "(Ljava/lang/String;)Ljavax/crypto/SecretKeyFactory;"));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(DESKeySpec.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex + 1));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(DESKeySpec.class), "<init>", "([B)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(SecretKeyFactory.class), "generateSecret", "(Ljava/security/spec/KeySpec;)Ljavax/crypto/SecretKey;"));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(IvParameterSpec.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new IntInsnNode(BIPUSH, 8));
        decryptInsts.add(new IntInsnNode(NEWARRAY, T_BYTE));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(IvParameterSpec.class), "<init>", "([B)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(Cipher.class), "init", "(ILjava/security/Key;Ljava/security/spec/AlgorithmParameterSpec;)V"));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(Cipher.class), "doFinal", "([B)[B"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ASTORE, startIndex));
        decryptInsts.add(new InsnNode(ARRAYLENGTH));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 3));
        decryptInsts.add(new LdcInsnNode("SHA-256"));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(MessageDigest.class), "getInstance", "(Ljava/lang/String;)Ljava/security/MessageDigest;"));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "reset", "()V"));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 3));
        decryptInsts.add(new IntInsnNode(BIPUSH, 32));
        decryptInsts.add(new InsnNode(ISUB));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 2));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "update", "([BII)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(MessageDigest.class), "digest", "()[B"));
        decryptInsts.add(new VarInsnNode(ASTORE, startIndex + 1));

        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 4));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 5));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 6));
        start = new LabelNode();
        decryptInsts.add(start);
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 5));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 4));
        decryptInsts.add(new InsnNode(IADD));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex + 1));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 4));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new InsnNode(IXOR));
        decryptInsts.add(new InsnNode(IOR));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 5));
        decryptInsts.add(new IincInsnNode(startIndex + 4, 1));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 4));
        decryptInsts.add(new IntInsnNode(BIPUSH, 32));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 5));
        decryptInsts.add(new JumpInsnNode(IFNE, realMethodStart));

        decryptInsts.add(ASMUtils.getNumberInsn(strings.size()));
        decryptInsts.add(new TypeInsnNode(ANEWARRAY, Type.getInternalName(Object.class)));
        if (dummys.isEmpty())
            decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ASTORE, startIndex + 1));
        if (dummys.isEmpty())
            decryptInsts.add(new FieldInsnNode(PUTSTATIC, ownerName, decryptedStringsFieldName, "Ljava/lang/Object;"));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 3));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 2));
        start = new LabelNode();
        decryptInsts.add(start);

        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new IincInsnNode(startIndex + 2, 1));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new IincInsnNode(startIndex + 2, 1));
        decryptInsts.add(new InsnNode(BALOAD));
        decryptInsts.add(new IntInsnNode(SIPUSH, 0xFF));
        decryptInsts.add(new InsnNode(IAND));
        decryptInsts.add(new IntInsnNode(BIPUSH, 8));
        decryptInsts.add(new InsnNode(ISHL));
        decryptInsts.add(new InsnNode(IOR));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 4));
        decryptInsts.add(new IntInsnNode(NEWARRAY, T_BYTE));
        decryptInsts.add(new VarInsnNode(ASTORE, startIndex + 5));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex + 5));
        decryptInsts.add(new InsnNode(ICONST_0));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 4));
        decryptInsts.add(new MethodInsnNode(INVOKESTATIC, Type.getInternalName(System.class), "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V"));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 4));
        decryptInsts.add(new InsnNode(IADD));
        decryptInsts.add(new VarInsnNode(ISTORE, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex + 1));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 6));
        decryptInsts.add(new IincInsnNode(startIndex + 6, 1));
        decryptInsts.add(new TypeInsnNode(NEW, Type.getInternalName(String.class)));
        decryptInsts.add(new InsnNode(DUP));
        decryptInsts.add(new VarInsnNode(ALOAD, startIndex + 5));
        decryptInsts.add(new LdcInsnNode("UTF-8"));
        decryptInsts.add(new MethodInsnNode(INVOKESPECIAL, Type.getInternalName(String.class), "<init>", "([BLjava/lang/String;)V"));
        decryptInsts.add(new MethodInsnNode(INVOKEVIRTUAL, Type.getInternalName(String.class), "intern", "()Ljava/lang/String;"));
        decryptInsts.add(new InsnNode(AASTORE));

        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 2));
        decryptInsts.add(new VarInsnNode(ILOAD, startIndex + 3));
        decryptInsts.add(new JumpInsnNode(IF_ICMPNE, start));

        decryptInsts.add(generateDummy(dummys, shuffled, strings, ownerName, decryptedStringsFieldName, startIndex));

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
            insts.add(ASMUtils.getNumberInsn(j | (rand.nextInt() & 0xFFFFFF00)));
            insts.add(new JumpInsnNode(GOTO, end));
        }
        LabelNode def = new LabelNode();
        insts.add(def);
        insts.add(ASMUtils.getNumberInsn(rand.nextInt()));
        insts.add(end);
        insts.insertBefore(idx[0], new TableSwitchInsnNode(0, 255, def, idx));
        return insts;
    }

    private InsnList generateDummy(List<FieldNode> dummys, List<String> shuffle, List<String> origin, String owner, String fieldName, int startIndex) {
        final InsnList insnList = new InsnList();
        if (dummys.isEmpty()) return insnList;
        Collections.shuffle(dummys); // dummy field也打乱 防止鉴定

        List<List<String>> restore = new ArrayList<>();

        for (int i = 0; i < dummys.size(); i++) {
            final List<String> shuffleList = new ArrayList<>(shuffle);
            Collections.shuffle(shuffleList);
            restore.add(shuffleList);
        }

        int varIn = 0;
        final int realVar = RandomUtils.getRandomInt(0, shuffle.size() - 1);
        for (List<String> strings : restore) {
            if (realVar == varIn) { // insert real local
                insnList.add(ASMUtils.getNumberInsn(shuffle.size()));
                insnList.add(new TypeInsnNode(ANEWARRAY, Type.getInternalName(Object.class)));
                insnList.add(new VarInsnNode(ASTORE, startIndex + varIn + 7));
                int conVar = 0;
                for (String string : origin) {
                    final int i = shuffle.indexOf(string);
                    insnList.add(new VarInsnNode(ALOAD, startIndex + varIn + 7));
                    insnList.add(ASMUtils.getNumberInsn(conVar));
                    insnList.add(new VarInsnNode(ALOAD, startIndex + 1));
                    insnList.add(ASMUtils.getNumberInsn(i));
                    insnList.add(new InsnNode(AALOAD));
                    insnList.add(new InsnNode(AASTORE));
                    conVar++;
                }
                varIn++;
            }
            insnList.add(ASMUtils.getNumberInsn(shuffle.size()));
            insnList.add(new TypeInsnNode(ANEWARRAY, Type.getInternalName(Object.class)));
            insnList.add(new VarInsnNode(ASTORE, startIndex + varIn + 7));
            int conVar = 0;
            for (String string : strings) {
                final int i = shuffle.indexOf(string);
                insnList.add(new VarInsnNode(ALOAD, startIndex + varIn + 7));
                insnList.add(ASMUtils.getNumberInsn(conVar));
                insnList.add(new VarInsnNode(ALOAD, startIndex + 1));
                insnList.add(ASMUtils.getNumberInsn(i));
                insnList.add(new InsnNode(AALOAD));
                insnList.add(new InsnNode(AASTORE));
                conVar++;
            }
            varIn++;
        }

        varIn = 0;
        for (FieldNode dummy : dummys) {
            if (varIn == realVar) {
                insnList.add(new VarInsnNode(ALOAD, startIndex + varIn + 7));
                insnList.add(new FieldInsnNode(PUTSTATIC, owner, fieldName, "Ljava/lang/Object;"));
                varIn++;
            }
            insnList.add(new VarInsnNode(ALOAD, startIndex + varIn + 7));
            insnList.add(new FieldInsnNode(PUTSTATIC, owner, dummy.name, "Ljava/lang/Object;"));
            varIn++;
        }

        return insnList;
    }

    private List<String> shuffleString(List<String> origin) {
        final List<String> shuffle = new ArrayList<>(origin);
        Collections.shuffle(shuffle);
        return shuffle;
    }

    @Override
    public void preprocess() throws Exception {
    }

    @Override
    public String annotation() {
        return Type.getDescriptor(tech.skidonion.obfuscator.annotations.StringEncryption.class);
    }
}
