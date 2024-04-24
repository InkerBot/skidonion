package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;
import tech.skidonion.verification.crypto.ChaCha20;

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

    @Test
    void testDecryption() {
        int hash = 123456;
        byte[] key = new byte[32];
        byte[] src;
        byte[] dst;
        ChaCha20 crypto = new ChaCha20(key, Internals.nonce(), 4096);
        switch (hash) {
            case 123:
                break;
            case 123456:
                src = _encrypt_();
                dst = new byte[src.length];
                crypto.decrypt(dst, src, src.length);
                _defineClass_(dst, dst.length);

                src = _encrypt_();
                dst = new byte[src.length];
                crypto.decrypt(dst, src, src.length);
                _defineClass_(dst, dst.length);
                break;
        }
    }

    private static void _defineClass_(byte[] bytes, int length) {
    }

    private static byte[] _encrypt_() {
        return new byte[0];
    }
}