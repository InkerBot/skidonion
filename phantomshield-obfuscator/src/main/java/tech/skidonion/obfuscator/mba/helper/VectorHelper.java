package tech.skidonion.obfuscator.mba.helper;

import org.la4j.Vector;
import org.la4j.iterator.VectorIterator;

import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

public class VectorHelper {

    public static Vector mulRandomAndAdd(Vector s, Vector b, int bits) {
        VectorIterator it = b.iterator();
        BigInteger[] result = new BigInteger[b.length()];

        BigInteger f = new BigInteger(bits, ThreadLocalRandom.current());
        while (it.hasNext()) {
            double x = it.next();
            int i = it.index();

            result[i] = BigInteger.valueOf((long) x).multiply(f);
        }
        s = s.copy();
        for (int i = 0; i < s.length(); i++) {
            s.set(i, (double) BigIntHelper.keepSignedBits(result[i].add(BigInteger.valueOf((long) s.get(i))), bits).longValue());
        }
        return s;
    }

}
