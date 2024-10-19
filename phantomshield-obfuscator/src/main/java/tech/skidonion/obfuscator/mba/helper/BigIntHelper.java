package tech.skidonion.obfuscator.mba.helper;

import java.math.BigInteger;

public class BigIntHelper {

    public static BigInteger keepBits(BigInteger i, int n) {
        BigInteger m = BigInteger.ONE.shiftLeft(n);
        return i.mod(m);
    }

    public static BigInteger keepSignedBits(BigInteger i, int n) {
        BigInteger m = BigInteger.ONE.shiftLeft(n);
        BigInteger r = i.mod(m);
        if (i.testBit(n - 1)) {
            r = r.subtract(m);
        }
        return r;
    }

}
