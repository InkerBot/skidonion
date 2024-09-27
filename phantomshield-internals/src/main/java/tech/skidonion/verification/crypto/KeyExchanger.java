package tech.skidonion.verification.crypto;

import net.i2p.crypto.eddsa.math.Field;
import net.i2p.crypto.eddsa.math.FieldElement;
import net.i2p.crypto.eddsa.math.ed25519.Ed25519LittleEndianEncoding;
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable;
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec;

public class KeyExchanger {
    public static byte[] exchange(byte[] publicKey, byte[] privateKey) {
        /* copy the private key and make sure it's valid */
        EdDSAPrivateKeySpec spec = new EdDSAPrivateKeySpec(EdDSANamedCurveTable.ED_25519_CURVE_SPEC, privateKey);
        byte[] e = spec.geta();

        Field pk = new Field(256, publicKey, new Ed25519LittleEndianEncoding());

        FieldElement x1;
        FieldElement x2;
        FieldElement z2;
        FieldElement x3;
        FieldElement z3;
        FieldElement tmp0;
        FieldElement tmp1;

        int pos;
        int swap;
        int b;

        /* unpack the public key and convert edwards to montgomery */
        /* due to CodesInChaos: montgomeryX = (edwardsY + 1)*inverse(1 - edwardsY) mod p */
//        fe_frombytes(x1, public_key);
        x1 = pk.getQ();
//        fe_1(tmp1);
        tmp1 = pk.ONE;
//        fe_add(tmp0, x1, tmp1);
        tmp0 = x1.add(tmp1);
//        fe_sub(tmp1, tmp1, x1);
        tmp1 = tmp1.subtract(x1);
//        fe_invert(tmp1, tmp1);
        tmp1 = tmp1.invert();
//        fe_mul(x1, tmp0, tmp1);
        x1 = tmp0.multiply(tmp1);

//        fe_1(x2);
        x2 = pk.ONE;
//        fe_0(z2);
        z2 = pk.ZERO;
//        fe_copy(x3, x1);
        Field c = new Field(256, x1.toByteArray(), new Ed25519LittleEndianEncoding());
        x3 = c.getQ();


//        fe_1(z3);
        z3 = pk.ONE;

        swap = 0;
        for (pos = 254; pos >= 0; --pos) {
            b = ((int) e[pos / 8] & 0xFF) >>> (pos & 7);
            b &= 1;
            swap ^= b;
//            fe_cswap(x2, x3, swap);
            x2 = x2.cswap(x3, swap);
//            fe_cswap(z2, z3, swap);
            z2 = z2.cswap(z3, swap);
            swap = b;

//            /* from montgomery.h */
//            fe_sub(tmp0, x3, z3);
            tmp0 = x3.subtract(z3);
//            fe_sub(tmp1, x2, z2);
            tmp1 = x2.subtract(z2);
//            fe_add(x2, x2, z2);
            x2 = x2.add(z2);
//            fe_add(z2, x3, z3);
            z2 = x3.add(z3);
//            fe_mul(z3, tmp0, x2);
            z3 = tmp0.multiply(x2);
//            fe_mul(z2, z2, tmp1);
            z2 = z2.multiply(tmp1);
//            fe_sq(tmp0, tmp1);
            tmp0 = tmp1.square();
//            fe_sq(tmp1, x2);
            tmp1 = x2.square();
//            fe_add(x3, z3, z2);
            x3 = z3.add(z2);
//            fe_sub(z2, z3, z2);
            z2 = z3.subtract(z2);
//            fe_mul(x2, tmp1, tmp0);
            x2 = tmp1.multiply(tmp0);
//            fe_sub(tmp1, tmp1, tmp0);
            tmp1 = tmp1.subtract(tmp0);
//            fe_sq(z2, z2);
            z2 = z2.square();
//            fe_mul121666(z3, tmp1);
            z3 = tmp1.mul121666();
//            fe_sq(x3, x3);
            x3 = x3.square();
//            fe_add(tmp0, tmp0, z3);
            tmp0 = tmp0.add(z3);
//            fe_mul(z3, x1, z2);
            z3 = x1.multiply(z2);
//            fe_mul(z2, tmp1, tmp0);
            z2 = tmp1.multiply(tmp0);
        }

//        fe_cswap(x2, x3, swap);
        x2 = x2.cswap(x3, swap);
//        fe_cswap(z2, z3, swap);
        z2 = z2.cswap(z3, swap);


//        fe_invert(z2, z2);
        z2 = z2.invert();
//        fe_mul(x2, x2, z2);
        x2 = x2.multiply(z2);
//        fe_tobytes(shared_secret, x2);
        return x2.toByteArray();
    }

}
