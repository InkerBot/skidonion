package net.i2p.crypto.eddsa;

import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec;

import java.security.SecureRandom;

/**
 * Default keysize is 256 (Ed25519)
 */
public final class KeyPairGenerator {
    private EdDSAParameterSpec edParams;
    private SecureRandom random;
    private boolean initialized;


    public void initialize(SecureRandom random) {
        edParams = EdDSANamedCurveTable.ED_25519_CURVE_SPEC;
        this.random = random;
        initialized = true;
    }

    public EdKeyPair generateKeyPair() {
        if (!initialized)
            initialize(new SecureRandom());

        byte[] seed = new byte[edParams.getCurve().getField().getb() / 8];
        random.nextBytes(seed);

        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(seed, edParams);
        EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(privKey.getA(), edParams);

        return new EdKeyPair(new EdDSAPublicKey(pubKey), new EdDSAPrivateKey(privKey));
    }

    public EdKeyPair generateKeyPair(byte[] seed) {
        if (!initialized)
            initialize(new SecureRandom());

        EdDSAPrivateKeySpec privKey = new EdDSAPrivateKeySpec(seed, edParams);
        EdDSAPublicKeySpec pubKey = new EdDSAPublicKeySpec(privKey.getA(), edParams);

        return new EdKeyPair(new EdDSAPublicKey(pubKey), new EdDSAPrivateKey(privKey));
    }
}
