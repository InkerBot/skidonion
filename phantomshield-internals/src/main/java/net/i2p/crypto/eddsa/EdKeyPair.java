package net.i2p.crypto.eddsa;

import java.security.PrivateKey;
import java.security.PublicKey;

public final class EdKeyPair {


    /**
     * The private key.
     */
    private EdDSAPrivateKey privateKey;

    /**
     * The public key.
     */
    private EdDSAPublicKey publicKey;

    public EdKeyPair(EdDSAPublicKey publicKey, EdDSAPrivateKey privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }

    public EdDSAPublicKey getPublic() {
        return publicKey;
    }

    public EdDSAPrivateKey getPrivate() {
        return privateKey;
    }
}
