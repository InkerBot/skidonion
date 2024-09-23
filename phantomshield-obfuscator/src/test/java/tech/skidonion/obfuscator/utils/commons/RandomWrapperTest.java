package tech.skidonion.obfuscator.utils.commons;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RandomWrapperTest {

    @Test
    void testGenerate() {
        Random rand = new Random(114514);
        for (int i = 0; i < 10000; i++) {
            System.out.println(RandomWrapper.nextLong(rand, 100));
        }
    }
}