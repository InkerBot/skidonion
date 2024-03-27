package tech.skidonion.verification.utils;

import tech.skidonion.verification.crypto.ChaCha20;

public class Internals {
    public static String verificationServer() {
        return "http://localhost:8694/";
    }

    public static String publicKey() {
        return "MCowBQYDK2VwAyEAfZU0fSt8t0DWwlXSX4hF/TKN7NW+Z9CYy8/m3/Q5AAs=";
    }

    public static long softwareId() {
        return 1L;
    }

    public static byte[] getNonce() {
        return new byte[0];
    }

    public static void setNonce(byte[] NONCE) {
    }

    public static Object getCrypto() {
        return null;
    }

    public static void setCrypto(Object CRYPTO) {
    }

    public static String getVerifyToken() {
        return null;
    }

    public static void setVerifyToken(String verifyToken) {
    }

    public static byte[] getKey() {
        return new byte[0];
    }

    public static void setKey(byte[] KEY) {
    }

    public static String getUsername() {
        return null;
    }

    public static void setUsername(String USERNAME) {
    }

    public static long getUserId() {
        return 0;
    }

    public static void setUserId(long userId) {
    }

    public static byte[] getMagicKey() {
        return new byte[0];
    }

    public static void setMagicKey(byte[] magicKey) {
    }

}
