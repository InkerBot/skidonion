package tech.skidonion.obfuscator.utils.commons;

import java.util.Random;

public class RandomWrapper {

    public static int nextInt(Random rand, int origin, int bound) {
        return (int) (rand.nextDouble() * (bound - origin)) + origin;
    }

    public static int nextInt(Random rand, int bound) {
        return nextInt(rand, 0, bound);
    }

    public static long nextLong(Random rand, long origin, long bound) {
        return (long) ((rand.nextDouble() * (bound - origin)) + origin);
    }

    public static long nextLong(Random rand, long bound) {
        return nextLong(rand, 0, bound);
    }
}
