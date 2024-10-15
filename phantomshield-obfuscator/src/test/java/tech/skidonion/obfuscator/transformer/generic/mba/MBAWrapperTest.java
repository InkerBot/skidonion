package tech.skidonion.obfuscator.transformer.generic.mba;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.obfuscate.ObfuscationConfig;
import tech.skidonion.obfuscator.utils.ASMUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

class MBAWrapperTest implements Opcodes {

    @Test
    void obfuscate() throws IOException {
        MBAWrapper obfuscated = MBAWrapper.obfuscate(Expr.fromString("3*x*(x-1)+y").get(), ObfuscationConfig.defaultConfig());
        obfuscated.setMethodLocalsIndex(5);
        int val1 = ThreadLocalRandom.current().nextInt();
        int val2 = ThreadLocalRandom.current().nextInt();
        int val3 = 5;
        int val4 = 4;

        obfuscated.set("aux0", MBAValue.local(1));
        obfuscated.set("aux1", MBAValue.local(2));
        obfuscated.set("x", MBAValue.local(3));
        obfuscated.set("y", MBAValue.local(4));

        ClassNode clz = new ClassNode();
        clz.version = V1_8;
        clz.access = ACC_PUBLIC;
        clz.name = "GeneratedMBA";
        clz.superName = "java/lang/Object";

        MethodNode method = new MethodNode(ACC_PUBLIC | ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
        InsnList __ = new InsnList();
        __.add(ASMUtils.getNumberInsn(val4));
        __.add(ASMUtils.getNumberInsn(val3));
        __.add(ASMUtils.getNumberInsn(val2));
        __.add(ASMUtils.getNumberInsn(val1));
        __.add(new VarInsnNode(ISTORE, 1));
        __.add(new VarInsnNode(ISTORE, 2));
        __.add(new VarInsnNode(ISTORE, 3));
        __.add(new VarInsnNode(ISTORE, 4));
        __.add(new FieldInsnNode(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        __.add(obfuscated.generate());
        __.add(new MethodInsnNode(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false));
        __.add(new InsnNode(RETURN));
        method.instructions.add(__);
        clz.methods.add(method);

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        clz.accept(writer);

        Files.write(Paths.get("GeneratedMBA.class"), writer.toByteArray());
    }

    @Test
    void testGenerated() {
        int n = 144710206;
        int n2 = 436745645;
        int x = 5;
        int y = 4;
        int n5 = (309652066 + Integer.MIN_VALUE * ~(~y ^ (n | n2)) + -160611163 * n2 + -313671510 * x + -397643808 * x + -1117811288 * (y | y) + 135736110 * ~(x ^ x | n2 & n) + -923239582 * ~(~(n2 & x)) + 1200648542 * n + -1986872485 * ~(~n2) + -404644989 * ~((n ^ n2) & n) + 1407297140 * (n & (n2 ^ (x ^ y))) + 1289876266 * ~(~x) + 578560945 * ~(~(~x)) + -55466769 * ~(n ^ ~n) + -781092965 * (n & (n2 & n2) & ~(~n)) + -1029672360 * y + -826009584 * ~(~(n & n2)) + -1407297140 * (n & (x ^ (y ^ n2))) + 542190117 * n + 923239582 * (n2 & n2 & (n2 ^ x) ^ n2)) * (-1509850038 + 429426309 * y + 274983793 * n + -681197619 * ~(~n) + 1849657644 * ~n + -2043681288 * n + -1612431440 * ~(~(n | x)) + -381497460 * y + -1747076241 * (n2 ^ n2 | n ^ x | x) + -47928849 * y + -559215839 * x + 559215840 * x + 4585462 * n + -524442667 * ((n2 ^ y ^ ~n2) & (y & x)) + 935459615 * ~(n2 & n | (x | n)));
        System.out.println(1148369639 + 1666673854 * (n5 | ~n5 | n2 ^ n2 ^ (n5 | n)) + Integer.MIN_VALUE * ((y | n | ~n) ^ x) + -729227266 * n2 + Integer.MIN_VALUE * (n ^ (~n5 | y)) + 870441614 * n + -1610146543 * n2 + 1623657129 * n + -2147483647 * y + -1968620844 * (n5 & n5 & n5 ^ (n2 & n5 | n5)) + 2059230106 * ~(n | n) + -1503829554 * (x ^ x) + -1955593487 * n2 + 1 * n5 + 200029771 * (~n | (n2 ^ n | (n | y))) + -630080444 * (~(n ^ y) ^ y) + Integer.MIN_VALUE * ~((n5 | y) ^ x & x) + 1082534567 * n);
        System.out.println(3 * x * (x - 1) + y);
    }


}