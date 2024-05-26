package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;

import java.io.Console;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class MachineIDUtilsTest {
    @Test
    void generate() {
        String[] a = new String[1];
        MachineIDUtils.generate(a);
        System.out.println(a[0]);
    }

    @Test
    void check() {
        for (int i = 0; i < 100; i++) {
            Object[] a = new Object[3];
            int rand = ThreadLocalRandom.current().nextInt();
            a[1] =
                    a[2] = "abcd";
//            MachineIDUtils.generate(a);
            MachineIDUtils.check(a);
            System.out.println(((long) a[0] >> 32 ^ rand) & 0b1);
        }

    }
}