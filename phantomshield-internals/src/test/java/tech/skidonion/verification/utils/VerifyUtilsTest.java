package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {
    @Test
    void testLogin() {
        System.out.println(VerifyUtils.login("imfl0wow", "fyx.flix1324") >> 8 & 0xFF);
    }
}