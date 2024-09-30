package tech.skidonion.verification.utils;

import net.i2p.crypto.eddsa.EdDSAEngine;
import net.i2p.crypto.eddsa.EdDSAPublicKey;
import net.i2p.crypto.eddsa.EdKeyPair;
import net.i2p.crypto.eddsa.KeyPairGenerator;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;
import tech.skidonion.verification.crypto.KeyExchanger;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

public class Internals {


    public static String verificationServer() {
        return "http://localhost:8694/";
//        return "https://skidonion.tech/";
    }

    public static byte[] publicKey() {
        try {
            byte[] dummy = new byte[32 * 4];
            EdDSAPublicKey pk = new EdDSAPublicKey(Base64.getDecoder().decode("MCowBQYDK2VwAyEAfZU0fSt8t0DWwlXSX4hF/TKN7NW+Z9CYy8/m3/Q5AAs="));
            System.arraycopy(pk.getAbyte(), 0, dummy, 0, 32);
            return dummy;
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static long softwareId() {
        return 1L;
    }

    public static void decryptClasses(int hash, byte[] key) {
    }

    public static byte[] sessionKey() {
        return new byte[16];
    }

    public static byte[] nonce() {
        return new byte[12];
    }

    public static String version() {
        return "";
    }

    public static void initBuffer() {
    }

    public static void polyXor(int[] encoded, byte[] decoded) {
        for (int i = 0, temp; i < encoded.length; i++) {
            temp = encoded[i];
            temp -= 0x713016e3;
            temp = (temp << 0x7) | (temp >>> 0x19);
            temp = ~temp;
            temp -= 0x954e2344;
            temp = (temp >>> 0x1b) | (temp << 0x5);
            temp = (temp >>> 0x12) | (temp << 0xe);
            temp -= 0x285123e0;
            temp += 0x683d2be5;
            temp += 0x30c22eac;
            temp ^= 0xe95bb198;
            temp ^= 0xd3637620;
            temp = ~temp;
            temp -= 0x4c25a002;
            temp = (temp >>> 0x1a) | (temp << 0x6);
            temp += 0xbde229f2;
            temp ^= 0x90ce88e2;
            decoded[i] = (byte) (temp & 0xff);
        }
    }

    public static boolean shouldKeepAlive() {
        return true;
    }

    public static boolean shouldCheckHwid() {
        return true;
    }

    public static boolean ed25519verify(ByteBuffer buffer) {
        try {
            buffer.position(0);
            EdDSAEngine verify = new EdDSAEngine();
            byte[] pk = new byte[32];
            byte[] sig = new byte[64];
            buffer.get(pk);
            buffer.position(32 * 4);
            buffer.get(sig);
            int length = buffer.get() & 0xff | (buffer.get() & 0xff) << 8 | (buffer.get() & 0xff) << 16 | (buffer.get() & 0xff) << 24;
            byte[] data = new byte[length];
            buffer.get(data);
            verify.initVerify(new EdDSAPublicKey(new EdDSAPublicKeySpec(pk, EdDSANamedCurveTable.ED_25519_CURVE_SPEC)));
            return verify.verify(data, sig);
        } catch (Exception e) {
            return false;
        }
    }

    public static void ed25519exchange(ByteBuffer buffer) {
        try {
            buffer.position(0);
            byte[] pk = new byte[32];
            byte[] sk = new byte[64];
            buffer.get(pk);
            buffer.position(32 * 4);
            buffer.get(sk);
            byte[] k = KeyExchanger.exchange(pk, sk);
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(k);
            buffer.position(32);
            buffer.put(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static void ed25519generate(ByteBuffer buffer) {
        KeyPairGenerator keyGen = new KeyPairGenerator();
        byte[] seed = new byte[32];
        buffer.position(0);
        buffer.get(seed);
        EdKeyPair edKeyPair = keyGen.generateKeyPair(seed);
        buffer.position(0);
        buffer.put(edKeyPair.getPublic().getAbyte());
        buffer.put(edKeyPair.getPrivate().getH());
    }

}
