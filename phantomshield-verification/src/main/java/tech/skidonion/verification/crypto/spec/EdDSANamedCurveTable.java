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

import tech.skidonion.verification.crypto.Utils;
import tech.skidonion.verification.crypto.math.Curve;
import tech.skidonion.verification.crypto.math.Field;
import tech.skidonion.verification.crypto.math.ed25519.Ed25519LittleEndianEncoding;
import tech.skidonion.verification.crypto.math.ed25519.Ed25519ScalarOps;
import tech.skidonion.obfuscator.annotations.NativeObfuscation;

/**
 * The named EdDSA curves.
 *
 * @author str4d
 */
@NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_RED)
public class EdDSANamedCurveTable {

    public static final EdDSANamedCurveSpec ED_25519_CURVE_SPEC;


    static {
        Field ed25519field = new Field(
                256, // b
                Utils.hexToBytes("edffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff7f"), // q
                new Ed25519LittleEndianEncoding());
        Curve ed25519curve = new Curve(ed25519field,
                Utils.hexToBytes("a3785913ca4deb75abd841414d0a700098e879777940c78c73fe6f2bee6c0352"), // d
                ed25519field.fromByteArray(Utils.hexToBytes("b0a00e4a271beec478e42fad0618432fa7d7fb3d99004d2b0bdfc14f8024832b"))); // I
        ED_25519_CURVE_SPEC = new EdDSANamedCurveSpec(
                "Ed25519",
                ed25519curve,
                "SHA-512", // H
                new Ed25519ScalarOps(), // l
                ed25519curve.createPoint( // B
                        Utils.hexToBytes("5866666666666666666666666666666666666666666666666666666666666666"),
                        true)); // Precompute tables for B
    }

}
