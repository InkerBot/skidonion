/**
 * EdDSA-Java by str4d
 * <p>
 * To the extent possible under law, the person who associated CC0 with
 * EdDSA-Java has waived all copyright and related or neighboring rights
 * to EdDSA-Java.
 * <p>
 * You should have received a copy of the CC0 legalcode along with this
 * work. If not, see <https://creativecommons.org/publicdomain/zero/1.0/>.
 */
package net.i2p.crypto.eddsa;

import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

import net.i2p.crypto.eddsa.math.Curve;
import net.i2p.crypto.eddsa.math.GroupElement;
import net.i2p.crypto.eddsa.math.ScalarOps;

/**
 * Signing and verification for EdDSA.
 * <p>
 * The EdDSA sign and verify algorithms do not interact well with
 * the Java Signature API, as one or more update() methods must be
 * called before sign() or verify(). Using the standard API,
 * this implementation must copy and buffer all data passed in
 * via update().
 * </p><p>
 * This implementation offers two ways to avoid this copying,
 * but only if all data to be signed or verified is available
 * in a single byte array.
 * </p><p>
 * Option 1:
 * </p><ol>
 * <li>Call initSign() or initVerify() as usual.
 * </li><li>Call setParameter(ONE_SHOT_MODE)
 * </li><li>Call update(byte[]) or update(byte[], int, int) exactly once
 * </li><li>Call sign() or verify() as usual.
 * </li><li>If doing additional one-shot signs or verifies with this object, you must
 * call setParameter(ONE_SHOT_MODE) each time
 * </li></ol>
 *
 * <p>
 * Option 2:
 * </p><ol>
 * <li>Call initSign() or initVerify() as usual.
 * </li><li>Call one of the signOneShot() or verifyOneShot() methods.
 * </li><li>If doing additional one-shot signs or verifies with this object,
 * just call signOneShot() or verifyOneShot() again.
 * </li></ol>
 *
 * @author str4d
 */
public final class EdDSAEngine {
    public static final String SIGNATURE_ALGORITHM = "NONEwithEdDSA";

    private MessageDigest digest;
    private EdDSAKey key;

    /**
     * To efficiently sign or verify data in one shot, pass this to setParameters()
     * after initSign() or initVerify() but BEFORE THE FIRST AND ONLY
     * update(data) or update(data, off, len). The data reference will be saved
     * and then used in sign() or verify() without copying the data.
     * Violate these rules and you will get a SignatureException.
     */
    public static final AlgorithmParameterSpec ONE_SHOT_MODE = new OneShotSpec();

    private static class OneShotSpec implements AlgorithmParameterSpec {
    }

    private void reset() {
        if (digest != null)
            digest.reset();
    }

    public void initSign(EdDSAPrivateKey privateKey) throws InvalidKeyException {
        reset();
        EdDSAPrivateKey privKey = (EdDSAPrivateKey) privateKey;
        key = privKey;

        if (digest == null) {
            // Instantiate the digest from the key parameters
            try {
                digest = MessageDigest.getInstance(key.getParams().getHashAlgorithm());
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeyException("cannot get required digest " + key.getParams().getHashAlgorithm() + " for private key.");
            }
        } else if (!key.getParams().getHashAlgorithm().equals(digest.getAlgorithm()))
            throw new InvalidKeyException("Key hash algorithm does not match chosen digest");
        digestInitSign(privKey);
    }

    private void digestInitSign(EdDSAPrivateKey privKey) {
        // Preparing for hash
        // r = H(h_b,...,h_2b-1,M)
        int b = privKey.getParams().getCurve().getField().getb();
        digest.update(privKey.getH(), b / 8, b / 4 - b / 8);
    }

    public void initVerify(EdDSAPublicKey publicKey) throws InvalidKeyException {
        reset();
        key = (EdDSAPublicKey) publicKey;

        if (digest == null) {
            // Instantiate the digest from the key parameters
            try {
                digest = MessageDigest.getInstance(key.getParams().getHashAlgorithm());
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeyException("cannot get required digest " + key.getParams().getHashAlgorithm() + " for private key.");
            }
        } else if (!key.getParams().getHashAlgorithm().equals(digest.getAlgorithm()))
            throw new InvalidKeyException("Key hash algorithm does not match chosen digest");
    }


    private byte[] x_engineSign(byte[] message, int offset, int length) throws SignatureException {
        Curve curve = key.getParams().getCurve();
        ScalarOps sc = key.getParams().getScalarOps();
        byte[] a = ((EdDSAPrivateKey) key).geta();

        // r = H(h_b,...,h_2b-1,M)
        digest.update(message, offset, length);
        byte[] r = digest.digest();

        // r mod l
        // Reduces r from 64 bytes to 32 bytes
        r = sc.reduce(r);

        // R = rB
        GroupElement R = key.getParams().getB().scalarMultiply(r);
        byte[] Rbyte = R.toByteArray();

        // S = (r + H(Rbar,Abar,M)*a) mod l
        digest.update(Rbyte);
        digest.update(((EdDSAPrivateKey) key).getAbyte());
        digest.update(message, offset, length);
        byte[] h = digest.digest();
        h = sc.reduce(h);
        byte[] S = sc.multiplyAndAdd(h, a, r);

        // R+S
        int b = curve.getField().getb();
        ByteBuffer out = ByteBuffer.allocate(b / 4);
        out.put(Rbyte).put(S);
        return out.array();
    }


    private boolean x_engineVerify(byte[] data, int off, int len, byte[] sigBytes) throws SignatureException {
        Curve curve = key.getParams().getCurve();
        int b = curve.getField().getb();
        if (sigBytes.length != b / 4)
            throw new SignatureException("signature length is wrong");

        // R is first b/8 bytes of sigBytes, S is second b/8 bytes
        digest.update(sigBytes, 0, b / 8);
        digest.update(((EdDSAPublicKey) key).getAbyte());
        // h = H(Rbar,Abar,M)
        digest.update(data, off, len);
        byte[] h = digest.digest();

        // h mod l
        h = key.getParams().getScalarOps().reduce(h);

        byte[] Sbyte = Arrays.copyOfRange(sigBytes, b / 8, b / 4);
        // R = SB - H(Rbar,Abar,M)A
        GroupElement R = key.getParams().getB().doubleScalarMultiplyVariableTime(
                ((EdDSAPublicKey) key).getNegativeA(), h, Sbyte);

        // Variable time. This should be okay, because there are no secret
        // values used anywhere in verification.
        byte[] Rcalc = R.toByteArray();
        for (int i = 0; i < Rcalc.length; i++) {
            if (Rcalc[i] != sigBytes[i])
                return false;
        }
        return true;
    }

    /**
     * To efficiently sign all the data in one shot, if it is available,
     * use this method, which will avoid copying the data.
     * <p>
     * Same as:
     * <pre>
     *  setParameter(ONE_SHOT_MODE)
     *  update(data)
     *  sig = sign()
     * </pre>
     *
     * @param data the message to be signed
     * @return the signature
     * @throws SignatureException if update() already called
     * @see #ONE_SHOT_MODE
     */
    public byte[] sign(byte[] data) throws SignatureException {
        return sign(data, 0, data.length);
    }

    /**
     * To efficiently sign all the data in one shot, if it is available,
     * use this method, which will avoid copying the data.
     * <p>
     * Same as:
     * <pre>
     *  setParameter(ONE_SHOT_MODE)
     *  update(data, off, len)
     *  sig = sign()
     * </pre>
     *
     * @param data byte array containing the message to be signed
     * @param off  the start of the message inside data
     * @param len  the length of the message
     * @return the signature
     * @throws SignatureException if update() already called
     * @see #ONE_SHOT_MODE
     */
    public byte[] sign(byte[] data, int off, int len) throws SignatureException {
        try {
            return x_engineSign(data, off, len);
        } finally {
            reset();
            // must leave the object ready to sign again with
            // the same key, as required by the API
            EdDSAPrivateKey privKey = (EdDSAPrivateKey) key;
            digestInitSign(privKey);
        }
    }

    /**
     * To efficiently verify all the data in one shot, if it is available,
     * use this method, which will avoid copying the data.
     * <p>
     * Same as:
     * <pre>
     *  setParameter(ONE_SHOT_MODE)
     *  update(data)
     *  ok = verify(signature)
     * </pre>
     *
     * @param data      the message that was signed
     * @param signature of the message
     * @return true if the signature is valid, false otherwise
     * @throws SignatureException if update() already called
     * @see #ONE_SHOT_MODE
     */
    public boolean verify(byte[] data, byte[] signature) throws SignatureException {
        return verify(data, 0, data.length, signature, 0, signature.length);
    }

    /**
     * To efficiently verify all the data in one shot, if it is available,
     * use this method, which will avoid copying the data.
     * <p>
     * Same as:
     * <pre>
     *  setParameter(ONE_SHOT_MODE)
     *  update(data, off, len)
     *  ok = verify(signature)
     * </pre>
     *
     * @param data      byte array containing the message that was signed
     * @param off       the start of the message inside data
     * @param len       the length of the message
     * @param signature of the message
     * @return true if the signature is valid, false otherwise
     * @throws SignatureException if update() already called
     * @see #ONE_SHOT_MODE
     */
    public boolean verify(byte[] data, int off, int len, byte[] signature) throws SignatureException {
        return verify(data, off, len, signature, 0, signature.length);
    }

    /**
     * To efficiently verify all the data in one shot, if it is available,
     * use this method, which will avoid copying the data.
     * <p>
     * Same as:
     * <pre>
     *  setParameter(ONE_SHOT_MODE)
     *  update(data)
     *  ok = verify(signature, sigoff, siglen)
     * </pre>
     *
     * @param data      the message that was signed
     * @param signature byte array containing the signature
     * @param sigoff    the start of the signature
     * @param siglen    the length of the signature
     * @return true if the signature is valid, false otherwise
     * @throws SignatureException if update() already called
     * @see #ONE_SHOT_MODE
     */
    public boolean verify(byte[] data, byte[] signature, int sigoff, int siglen) throws SignatureException {
        return verify(data, 0, data.length, signature, sigoff, siglen);
    }


    public boolean verify(byte[] data, int off, int len, byte[] signature, int offset, int length)
            throws SignatureException {
        if (signature == null) {
            throw new IllegalArgumentException("signature is null");
        }
        if (offset < 0 || length < 0) {
            throw new IllegalArgumentException
                    ("offset or length is less than 0");
        }
        if (signature.length - offset < length) {
            throw new IllegalArgumentException
                    ("signature too small for specified offset and length");
        }

        return engineVerify(data, off, len, signature, offset, length);
    }

    private boolean engineVerify(byte[] data, int off, int len, byte[] sigBytes, int offset, int length)
            throws SignatureException {
        byte[] sigBytesCopy = new byte[length];
        System.arraycopy(sigBytes, offset, sigBytesCopy, 0, length);
        try {
            return x_engineVerify(data, off, len, sigBytesCopy);
        } finally {
            reset();
        }
    }
}
