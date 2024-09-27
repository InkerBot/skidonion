package tech.skidonion.verification.utils;

import org.junit.jupiter.api.Test;
import tech.skidonion.verification.crypto.ChaCha20;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {

    @Test
    void testPoly() {
        byte[] raw = {(byte) 0xff, 1, 2, 3, (byte) 0xff, 0, 0, 0, (byte) 0xff, 0, 0, 0, (byte) 0xff, 0, 0, 0};
        for (int i = 0; i < 4; i++) {
            System.out.println((raw[i * 4] & 0xff) | ((raw[i * 4 + 1] & 0xff) << 8) | ((raw[i * 4 + 2]) << 16) | (raw[i * 4 + 3] << 24));
        }
    }

    @Test
    void getIP() {
        System.out.println(HttpUtils.post("https://who.nie.netease.com/", null));
    }

    @Test
    void testLogin() {
        System.out.println((byte) (VerifyUtils.login("imfl0wow", "41aae1446c6c0190a97de6a73d35fc3b", true) >> 8 & 0xFF));

        System.out.println(VerifyUtils.getUserId());
        System.out.println(VerifyUtils.getUsername());
        System.out.println(VerifyUtils.getExpiredDate("授权验证用户组"));
        System.out.println(VerifyUtils.getExpiredDates());
        System.out.println(VerifyUtils.getNickname());

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
                decryptBuffer(key);
                src = _encrypt_();
                dst = crypto.xor(src);
                _defineClass_(dst, dst.length);

                src = _encrypt_();
                dst = crypto.xor(src);
                _defineClass_(dst, dst.length);
                break;
        }
    }

    private static void _defineClass_(byte[] bytes, int length) {
    }

    private static void decryptBuffer(byte[] key) {

    }

    private static byte[] _encrypt_() {
        return new byte[0];
    }
}