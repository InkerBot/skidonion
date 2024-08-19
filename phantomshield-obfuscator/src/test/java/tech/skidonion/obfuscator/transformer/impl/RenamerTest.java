package tech.skidonion.obfuscator.transformer.impl;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

class RenamerTest {

    @Test
    void biginteger() {
        BigInteger a = new BigInteger(new byte[]{0});
        for (int i = 0; i < 10000000; i++) {
            a.add(BigInteger.ONE);
        }
        System.out.println(a.longValue());
    }
}