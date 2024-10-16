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
        MBAWrapper obfuscated = MBAWrapper.obfuscate(Expr.fromString("x*y + y^(x >>> 16)").get(), ObfuscationConfig.defaultConfig());
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
        int var1 = 587731291;
        int var2 = -539766994;
        byte x = 5;
        byte y = 4;
        int var6 = (-1191601810 + 257351253 * ~(~x ^ var2 & var2) + -357737150 * (x & y & ~(var1 & var1)) + 1708659763 * (var2 | var1) + 98056955 * var2 + 1096175415 * (~var2 & ~var2 ^ (var2 ^ x) & ~var1) + -1180161112 * ((var2 | var2) ^ ~x ^ var1) + -1791330300 * ~((var2 | x) & ~var1) + 1968615073 * (x | y | y & y) + 1056222571 * var1 + 1968615073 * (y & x & (x | y) ^ ~(y & var1)) + -1062920979 * (x ^ var1) + 1968615073 * ((var1 | x) & var1 & x | ~(var1 | y)) + -1997198040 * ((x | var2) & (y | var2) & var2) + 594466968 * ((~var1 | x ^ var2) ^ (~var1 | var1 ^ x)) + 183402814 * (var1 ^ ~x | ~var2 | var1 ^ x) + 978067848 * (x ^ var1 | var2 | y | ~var2 ^ (y | y)) + -120019325 * ~var1 + -1410367656 * (x | var2) + -1627993687 * (x ^ var1 ^ var2)) * (1105288914 + -485610050 * (y | ~(var2 ^ var2)) + 1965178209 * (y & var2 ^ y ^ var2 | (y ^ var2) & y & var2) + 1289788104 * (var2 | y | var2 | y & var1) + 1947855020 * ~var2 + -2084413811 * (~(x & x) & ~var2 & var1 & x) + 469783510 * ~(~var2) + 1828397696 * (~x ^ ~var2 & y & var2) + 1040000985 * (y | ~(var2 & var2)) + -1040000984 * ~(var2 & x & var1 & var2) + 929412384 * ~var2 + -1040000984 * (var1 & var2 | ~var2 | y ^ x & var1) + 1370138768 * x + -520000492 * ((~var2 | ~var1) ^ ~(~y)) + -520000492 * ((~y | x ^ var1) & (var1 ^ var1 | ~var2)) + 520000492 * ((y | x) & (x ^ var2) ^ (var2 ^ y | var2 | var1)) + 520000492 * (x ^ (var2 | y) ^ ~var1) + 790977669 * x + -1554764661 * ~((var2 | x) & x) + -1887483402 * ~(~(x ^ var2)) + 520000492 * (y ^ x & y ^ y ^ var2));
        int var5 = 1160788869 + -1534335149 * var6 + -585824439 * var1 + 1309934770 * (x & ~var6) + 181120011 * y + 585824439 * var1 + -1433718562 * ~x + -1842263512 * y + -2105664761 * var6 + -654967385 * ~(~(x ^ var6)) + 1661143502 * y + -1700459865 * (~x | ~var1 | var1) + -2088685947 * (x | ~(~x));
        int var7 = -112775058 + 788005651 * var1 + 453905097 * ~var1 + -1186697274 * var2 + -2047123988 * (var2 | var1 | x | x & (y ^ y)) + -1153342674 * x + -878380821 * x + -334100554 * var1 + 870826785 * var2 + 1751273103 * ((~x ^ x) & x) + 1617127069 * x + -293336821 * y + 710447312 * ((var2 ^ x) & var2 | (var1 | x) ^ x ^ x) + 1336676676 * ((x ^ x ^ ~x) & (x | var1 | var2)) + -1182700761 * (~(y ^ x) ^ x) + -889363940 * y + 315870489 * (var2 | y ^ y) + 616020606 * (~(x ^ x) | var2 ^ x ^ y) >>> -67202643 + Integer.MIN_VALUE * (~(~var1) | ~var2 ^ (var2 | x)) + -795670707 * y + -1387502436 * ((~var2 | ~var1) & (var2 ^ y & var2)) + -1833723036 * ((var1 | y | x) & (var2 & x ^ var1 & var2)) + Integer.MIN_VALUE * (var1 & x ^ var1 ^ y ^ (var2 ^ x | var2 & x)) + -1519962424 * ((y & x | var1 ^ var2) & ~(x & var2)) + 214257128 * ~(~y) + -1696908002 * (~x & (var2 | x) & (~var2 ^ y)) + -1387502436 * ((var1 ^ var2 ^ x) & var1 & y & (y | var2)) + -852871346 * ((y ^ ~y) & ~(var1 & var2)) + -516245760 * var2 + 558774266 * ~(var2 & var2 | var2 | var2) + 269460788 * var2 + 530276044 * var1 + -1078096870 * ((var2 | y) & ((y | var1) ^ x ^ var1)) + 773046350 * (x & ((x | x) ^ var2)) + -487973199 * (y & (var1 | y | ~var1)) + 417997573 * var2 + 309405566 * ((~var1 | y) ^ (y ^ var2) & y) + 848391702 * ~(var2 | var1 | var1 ^ var1) + 309405566 * (y | x) + 751271120 * (y & (x | var1) & ~var1) + -1082451916 * ~(~(var2 ^ x)) + 1216580801 * (var2 ^ ~x ^ (x | x));
        System.out.println(-2043327943 + -2043327944 * ~((var5 ^ var5) & ~var5) + 2 * ~(~(var5 | var7)) + 1199710375 * (~var2 & (x ^ var2) & var1 & y & var2) + -990650124 * var5 + 990650123 * var5 + 1993647009 * ((var1 ^ var1) & ~var5 & (var7 ^ y ^ y)) + 1 * ~(~(~var7)));
        System.out.println(x*y + y^(x >>> 16));
    }


}