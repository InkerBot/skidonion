package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {
    @Test
    void testLogin() {
        System.out.println(VerifyUtils.login("imfl0wow", "fyx.flix1324") >> 8 & 0xFF);

        System.out.println(VerifyUtils.getUserId());
        System.out.println(VerifyUtils.getUsername());
        System.out.println(VerifyUtils.getExpiredDate("授权验证用户组"));

        VerifyUtils.heartbeat();

    }
}