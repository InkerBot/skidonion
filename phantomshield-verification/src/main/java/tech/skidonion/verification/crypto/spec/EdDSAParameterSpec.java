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
package tech.skidonion.verification.crypto.spec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import tech.skidonion.verification.crypto.math.Curve;
import tech.skidonion.verification.crypto.math.GroupElement;
import tech.skidonion.verification.crypto.math.ed25519.Ed25519ScalarOps;

/**
 * Parameter specification for an EdDSA algorithm.
 *
 * @author str4d
 */
public class EdDSAParameterSpec {
    private final Curve curve;
    private final String hashAlgo;
    private final Ed25519ScalarOps sc;
    private final GroupElement B;

    /**
     * @param curve    the curve
     * @param hashAlgo the JCA string for the hash algorithm
     * @param sc       the parameter L represented as ScalarOps
     * @param B        the parameter B
     * @throws IllegalArgumentException if hash algorithm is unsupported or length is wrong
     */
    public EdDSAParameterSpec(Curve curve, String hashAlgo,
                              Ed25519ScalarOps sc, GroupElement B) {
        try {
            MessageDigest hash = MessageDigest.getInstance(hashAlgo);
            // EdDSA hash function must produce 2b-bit output
            if (curve.getField().getb() / 4 != hash.getDigestLength())
                throw new IllegalArgumentException("Hash output is not 2b-bit");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Unsupported hash algorithm");
        }

        this.curve = curve;
        this.hashAlgo = hashAlgo;
        this.sc = sc;
        this.B = B;
    }

    public Curve getCurve() {
        return curve;
    }

    public String getHashAlgorithm() {
        return hashAlgo;
    }

    public Ed25519ScalarOps getScalarOps() {
        return sc;
    }

    /**
     * @return the base (generator)
     */
    public GroupElement getB() {
        return B;
    }

}
