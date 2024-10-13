package tech.skidonion.obfuscator.mba.helper;

public class MathHelper {

    public static long divEuclid(long a, long b) {
        long q = a / b;
        long r = a % b;
        if (r < 0) {
            if (b > 0) {
                return q - 1;
            } else {
                return q + 1;
            }
        } else {
            return q;
        }
    }


    public static long remEuclid(long a, long b) {
        long r = a % b;
        if (r < 0) {
            if (b > 0) {
                return r + b;
            } else {
                return r - b;
            }
        } else {
            return r;
        }
    }
}
