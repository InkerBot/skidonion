package tech.skidonion.obfuscator.utils;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class MachineIDUtilsTest {

    @Test
    void generate() {
        for (int i = 0; i < 10; i++) {
            Object[] array = new Object[3];
            array[0] = -1;
            array[1] = ThreadLocalRandom.current().nextInt();
            MachineIDUtils.generate(array);
            MachineIDUtils.check(array);
            System.out.println((((long) array[0] >> 32) ^ (int) array[1]) & 0b1);
        }
    }
}
