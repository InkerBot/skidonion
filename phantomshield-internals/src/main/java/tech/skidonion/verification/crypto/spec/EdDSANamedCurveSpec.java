package tech.skidonion.verification.crypto.spec;

import tech.skidonion.verification.crypto.math.Curve;
import tech.skidonion.verification.crypto.math.GroupElement;
import tech.skidonion.verification.crypto.math.ScalarOps;

/**
 * EdDSA Curve specification that can also be referred to by name.
 * @author str4d
 *
 */
public class EdDSANamedCurveSpec extends EdDSAParameterSpec {
    private final String name;

    public EdDSANamedCurveSpec(String name, Curve curve,
                               String hashAlgo, ScalarOps sc, GroupElement B) {
        super(curve, hashAlgo, sc, B);
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
