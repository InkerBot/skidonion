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
package tech.skidonion.verification.crypto;

import tech.skidonion.verification.crypto.math.Curve;
import tech.skidonion.verification.crypto.math.GroupElement;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.Arrays;

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

    private MessageDigest digest;
    private EdDSAPublicKey key;

    private void reset() {
        if (digest != null)
            digest.reset();
    }

    public void initVerify(EdDSAPublicKey publicKey) {
        reset();
        key = publicKey;

        if (digest == null) {
            // Instantiate the digest from the key parameters
            try {
                digest = MessageDigest.getInstance(key.getParams().getHashAlgorithm());
            } catch (NoSuchAlgorithmException e) {
            }
        }
    }


    private boolean x_engineVerify(byte[] data, int off, int len, byte[] sigBytes) {
        Curve curve = key.getParams().getCurve();
        int b = curve.getField().getb();
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

    public boolean verify(byte[] data, byte[] signature) {
        return verify(data, 0, data.length, signature, 0, signature.length);
    }

    public boolean verify(byte[] data, int off, int len, byte[] signature, int offset, int length) {
        return engineVerify(data, off, len, signature, offset, length);
    }

    private boolean engineVerify(byte[] data, int off, int len, byte[] sigBytes, int offset, int length) {
        byte[] sigBytesCopy = new byte[length];
        System.arraycopy(sigBytes, offset, sigBytesCopy, 0, length);
        try {
            return x_engineVerify(data, off, len, sigBytesCopy);
        } finally {
            reset();
        }
    }
}
