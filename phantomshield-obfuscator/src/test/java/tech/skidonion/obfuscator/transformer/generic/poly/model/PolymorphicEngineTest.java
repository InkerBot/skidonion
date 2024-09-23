package tech.skidonion.obfuscator.transformer.generic.poly.model;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.generic.poly.visitors.InstrumentsVisitor;
import tech.skidonion.obfuscator.transformer.generic.poly.visitors.JavaVisitor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

class PolymorphicEngineTest implements Opcodes {

    @Test
    void testGenerateSourceCode() {
        PolymorphicEngine engine = new PolymorphicEngine();
        engine.setUserRandom(new Random(114514));

        String stringToObfuscate = "Hello World!";
        Context ctx = engine.transform(stringToObfuscate.getBytes(StandardCharsets.UTF_8));
        JavaVisitor visitor = new JavaVisitor();  // or any other target
        System.out.println(visitor.visit(ctx));
    }

    @Test
    void testGenerateInstruments() throws IOException {
        PolymorphicEngine engine = new PolymorphicEngine();
        engine.setUserRandom(new Random(114514));

        Context ctx = engine.generateChain();
        InstrumentsVisitor visitor = new InstrumentsVisitor(3);  // or any other target
        ClassNode node = new ClassNode();
        node.name = "PolyTest";
        node.access = ACC_PUBLIC;
        node.superName = "java/lang/Object";
        node.version = V1_8;

        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        method.instructions.add(buildData());
        method.instructions.add(visitor.visit(ctx));
        method.instructions.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(INVOKESTATIC,"java/util/Arrays","toString","([B)Ljava/lang/String;", false));
        method.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,"java/io/PrintStream","println","(Ljava/lang/String;)V", false));
        method.instructions.add(new InsnNode(RETURN));

        node.methods.add(method);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);

        Files.write(Paths.get("PolyTest.class"), writer.toByteArray());
    }

    private static InsnList buildData() {
        InsnList in = new InsnList();
        in.add(new LabelNode());
        in.add(new IntInsnNode(BIPUSH, 12));
        in.add(new IntInsnNode(NEWARRAY, T_BYTE));
        in.add(new VarInsnNode(ASTORE, 1));
        in.add(new LabelNode());
        in.add(new IntInsnNode(BIPUSH, 12));
        in.add(new IntInsnNode(NEWARRAY, T_INT));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_0));
        in.add(new LdcInsnNode(-722756634));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_1));
        in.add(new LdcInsnNode(552311783));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_2));
        in.add(new LdcInsnNode(1156291559));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_3));
        in.add(new LdcInsnNode(1156291559));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_4));
        in.add(new LdcInsnNode(2028706791));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new InsnNode(ICONST_5));
        in.add(new LdcInsnNode(887856102));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 6));
        in.add(new LdcInsnNode(418094055));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 7));
        in.add(new LdcInsnNode(2028706791));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 8));
        in.add(new LdcInsnNode(1827380199));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 9));
        in.add(new LdcInsnNode(1156291559));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 10));
        in.add(new LdcInsnNode(619420647));
        in.add(new InsnNode(IASTORE));
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, 11));
        in.add(new LdcInsnNode(820747238));
        in.add(new InsnNode(IASTORE));
        in.add(new VarInsnNode(ASTORE, 2));
        in.add(new VarInsnNode(ALOAD, 2)); // encode
        in.add(new VarInsnNode(ALOAD, 1)); // decode
        return in;
    }

    @Test
    void print() {
        byte[] decoded = new byte[12];
        int[] encoded = {-722756634, 552311783, 1156291559, 1156291559, 2028706791, 887856102, 418094055, 2028706791, 1827380199, 1156291559, 619420647, 820747238};

        for (int i = 0, temp; i < encoded.length; i++) {
            temp = encoded[i];
            temp = (temp << 0x3) | (temp >>> 0x1d);
            temp ^= 0xc49482b4;
            temp = (temp << 0x1e) | (temp >>> 0x2);
            temp ^= 0xef14b686;
            temp = (temp >>> 0x9) | (temp << 0x17);
            temp = ~temp;
            temp ^= 0x9e0f04da;
            temp = (temp >>> 0xa) | (temp << 0x16);
            temp -= 0x269b1780;
            temp = (temp << 0x9) | (temp >>> 0x17);
            temp += 0x6d597a13;
            temp = (temp << 0x1f) | (temp >>> 0x1);
            temp = (temp >>> 0x1b) | (temp << 0x5);
            temp = ~temp;
            temp = ~temp;
            temp = (temp << 0xb) | (temp >>> 0x15);
            decoded[i] = (byte) (temp & 0xff);
        }
        System.out.println(Arrays.toString(decoded));
    }


}