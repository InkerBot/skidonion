package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;

import java.io.Console;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class MachineIDUtilsTest {

    @Test
    void testGenerateRandom() {
        int hashHome = System.getProperty("user.home").hashCode();
        Random rand = new Random(hashHome);
        String str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder result = new StringBuilder(".");
        for (int i = 0; i < 16; i++) {
            int number = rand.nextInt(str.length());
            result.append(str.charAt(number));
        }

        Path path = Paths.get(System.getProperty("user.home"), result.toString());
        System.out.println(path);
    }

    @Test
    void generate() {
        String[] a = new String[1];
        MachineIDUtils.generate(a);
        System.out.println(a[0]);
    }

    @Test
    void check() {

        int rand = ThreadLocalRandom.current().nextInt();
        Object[] array = new Object[5];
        array[0] = ThreadLocalRandom.current().nextInt();
        array[1] = ThreadLocalRandom.current().nextInt();
        array[2] = ThreadLocalRandom.current().nextInt();
        array[3] = rand;
        array[4] = "adfd55c05f7c100853f9ac8fca38ca5c1da98e88c35fc3411caba1cb9528bb54078ac6b9fc37c865149593bce01cac413fbfcffbf804f60b42b432e2555af0d2366f04fb90490ae3bff93fc46b4b4525c44c69b2d3a72c2b72d476e1ce00ca15fd9db00050a430c06232f27764ced2b07a22d23757597531aa8406ed846f7e";
        MachineIDUtils.check(array);
        MachineIDUtils.generate(array);
        MachineIDUtils.check(array);
        System.out.println((((Number) array[0]).longValue() >> 32 ^ rand) & 0b1);

    }
}