package tech.skidonion.obfuscator.transformer.impl;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;
import static org.objectweb.asm.Opcodes.*;

class NativeObfuscationTest {

    @Test
    void encryption() {
        byte[] des = new byte[32];


        byte[] magic = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        byte[] session = {-1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -12, -13, -14, -15, -16};

        ThreadLocalRandom.current().nextBytes(session);

        for (int i = des.length - 1; i >= 0; i--) {
            int index = i / 2;
            int position = index % 2;
            if (i % 2 == 0) {
                des[i] = magic[index + (position == 1 ? -1 : 1)];
            } else {
                des[i] = session[index + (position == 1 ? -1 : 1)];
            }
        }
        byte temp = des[0];
        des[0] = des[des.length - 1];
        des[des.length - 1] = temp;

        System.out.println(Arrays.toString(des));
    }

    @Test
    void buildDecryption() throws IOException {
        final Map<String, List<String>> encryptedClasses = new HashMap<>();
        encryptedClasses.put("123", new ArrayList<String>() {
            {
                add("abc");
                add("def");
                add("ghi");
            }
        });

        ClassNode node = new ClassNode();
        node.name = "test";
        node.version = V1_8;
        node.access = ACC_PUBLIC | ACC_SUPER;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode();
        method.name = "test";
        method.access = ACC_PUBLIC | ACC_STATIC;
        method.desc = "()V";
        InsnList insns = method.instructions;
        node.methods.add(method);
//        LDC 123456
        insns.add(new LdcInsnNode(123456));
//        BIPUSH 32
        insns.add(new IntInsnNode(BIPUSH, 32));
//        NEWARRAY T_BYTE
        insns.add(new IntInsnNode(NEWARRAY, T_BYTE));

        int locals = 0;
        int hashIndex = locals++; // istore 1
        int keyIndex = locals++; // astore 2
        int srcIndex = locals++; // astore 3
        int dstIndex = locals++; // astore 4
        int cryptoIndex = locals; // astore 5
        insns.add(new VarInsnNode(ASTORE, keyIndex)); // astore 2
        insns.add(new VarInsnNode(ISTORE, hashIndex)); // istore 1

//                                    NEW tech/skidonion/verification/crypto/ChaCha20
        insns.add(new TypeInsnNode(NEW, "tech/skidonion/verification/crypto/ChaCha20"));
//                                    DUP
        insns.add(new InsnNode(DUP));
//                                    ALOAD 2
        insns.add(new VarInsnNode(ALOAD, keyIndex));
//                                    INVOKESTATIC tech/skidonion/verification/utils/Internals.nonce ()[B
        insns.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/verification/utils/Internals", "nonce", "()[B", false));
//                                    SIPUSH 4096
        insns.add(new IntInsnNode(SIPUSH, 4096));
//                                    INVOKESPECIAL tech/skidonion/verification/crypto/ChaCha20.<init> ([B[BI)V
        insns.add(new MethodInsnNode(INVOKESPECIAL, "tech/skidonion/verification/crypto/ChaCha20", "<init>", "([B[BI)V", false));
//                                    ASTORE 5
        insns.add(new VarInsnNode(ASTORE, cryptoIndex));

//                                    ILOAD 1
        insns.add(new VarInsnNode(ILOAD, hashIndex));
//                                    LOOKUPSWITCH
//                                    123: L4
//                                    123456: L5
//                                    default: L6
        LabelNode defaultLabel = new LabelNode();
        Map<Integer, LabelNode> labelPool = new HashMap<>();
        Set<String> roles = encryptedClasses.keySet();
        int[] hashes = new int[roles.size()];
        LabelNode[] labels = new LabelNode[roles.size()];
        int i;
        i = 0;
        for (String s : roles) {
            labelPool.put(hashes[i] = s.hashCode(), labels[i++] = new LabelNode());
        }
        insns.add(new LookupSwitchInsnNode(defaultLabel, hashes, labels));

        i = 0;
        for (String roleName : roles) {
            insns.add(labels[i]);
            for (String cw : encryptedClasses.get(roleName)) {
                String classFileName = "data_" + StringUtils.escapeCppNameString(cw.replace('/', '_'));
//                                            INVOKESTATIC tech/skidonion/verification/utils/VerifyUtilsTest._encrypt_ ()[B
                insns.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_encrypt_" + classFileName, "()[B", false));
//                                            ASTORE 3
                insns.add(new VarInsnNode(ASTORE, srcIndex));
//                                            ALOAD 3
                insns.add(new VarInsnNode(ALOAD, srcIndex));
//                                            ARRAYLENGTH
                insns.add(new InsnNode(ARRAYLENGTH));
//                                            NEWARRAY T_BYTE
                insns.add(new IntInsnNode(NEWARRAY, T_BYTE));
//                                            ASTORE 4
                insns.add(new VarInsnNode(ASTORE, dstIndex));
//                                            ALOAD 5
                insns.add(new VarInsnNode(ALOAD, cryptoIndex));
//                                            ALOAD 4
                insns.add(new VarInsnNode(ALOAD, dstIndex));
//                                            ALOAD 3
                insns.add(new VarInsnNode(ALOAD, srcIndex));
//                                            ALOAD 3
                insns.add(new VarInsnNode(ALOAD, srcIndex));
//                                            ARRAYLENGTH
                insns.add(new InsnNode(ARRAYLENGTH));
//                                            INVOKEVIRTUAL tech/skidonion/verification/crypto/ChaCha20.decrypt ([B[BI)V
                insns.add(new MethodInsnNode(INVOKEVIRTUAL, "tech/skidonion/verification/crypto/ChaCha20", "decrypt", "([B[BI)V", false));
//                                            ALOAD 4
                insns.add(new VarInsnNode(ALOAD, dstIndex));
//                                            ALOAD 4
                insns.add(new VarInsnNode(ALOAD, dstIndex));
//                                            ARRAYLENGTH
                insns.add(new InsnNode(ARRAYLENGTH));
//                                            INVOKESTATIC tech/skidonion/verification/utils/VerifyUtilsTest._defineClass_ ([BI)V
                insns.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_defineClass_" + cw, "([BI)V"));
            }
            insns.add(new JumpInsnNode(GOTO, defaultLabel));
            i++;
        }
        insns.add(defaultLabel);

        insns.add(new InsnNode(RETURN));


        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(cw);
        Files.write(Paths.get("TestGenerateDecryption.class"), cw.toByteArray());
    }


    @Test
    void sortClasses() {
        List<Class<?>> clz = new ArrayList<>();
        clz.add(ClassA.class);
        clz.add(ClassB.class);
        clz.add(ClassC.class);
        clz.add(ClassD.class);

        clz.sort((a, b) -> {
            if (a.equals(b)) {
                return 0;
            } else if (a.isAssignableFrom(b)) {
                return -1;
            } else {
                return 1;
            }
        });

        System.out.println(clz);
    }

    static class ClassA extends ClassB implements ClassD {

    }

    static class ClassB implements ClassC {

    }

    interface ClassC {

    }

    interface ClassD {

    }


}