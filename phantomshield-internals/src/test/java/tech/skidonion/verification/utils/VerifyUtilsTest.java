package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {
    @Test
    void getIP() {
        System.out.println(HttpUtils.post("https://who.nie.netease.com/", null));
    }

    @Test
    void testLogin() {
        System.out.println(VerifyUtils.login("imfl0wow", "fyx.flix1324") >> 8 & 0xFF);

        System.out.println(VerifyUtils.getUserId());
        System.out.println(VerifyUtils.getUsername());
        System.out.println(VerifyUtils.getExpiredDate("授权验证用户组"));
        System.out.println(VerifyUtils.getExpiredDates());

        System.out.println(VerifyUtils.hasRole("授权验证用户组"));

        System.out.println(VerifyUtils.getVerifyToken());

        VerifyUtils.getCloudConstant("授权验证用户组".hashCode(), 0).ifPresent(System.out::println);
//        VerifyUtils.setAsSuspected("测试");
//        VerifyUtils.heartbeat();
    }
}