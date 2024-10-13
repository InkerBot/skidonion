package tech.skidonion.obfuscator.mba;


import org.la4j.Matrix;
import org.la4j.Vector;
import tech.skidonion.obfuscator.mba.helper.VectorHelper;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Lattice {
    private final Matrix basis;

    private Lattice(Matrix basis) {
        this.basis = basis;
    }

    public Matrix getBasis() {
        return basis;
    }

    /**
     * Creates an empty lattice.
     */
    public static Lattice empty() {
        return new Lattice(Matrix.zero(0, 0));
    }

    /**
     * Is the lattice empty?
     */
    public boolean isEmpty() {
        return this.basis.columns() == 0 && this.basis.rows() == 0;
    }

    /**
     * The lattice basis are the rows of the matrix.
     */
    public static Lattice from(Matrix basis) {
        return new Lattice(basis);
    }

    /**
     * The rows of the matrix generate the lattice
     * but are potentially linearly dependent.
     * This function will compute the Hermite normal form
     * and remove zero rows.
     */
    public static Lattice fromGeneratingSet(Matrix generatingSet) {
        Diophantine.hermiteNormalForm(generatingSet);
        int rank = (generatingSet.rows() - (int) IntStream.range(0, generatingSet.rows()).mapToObj(i -> generatingSet.getRow(generatingSet.rows() - 1 - i)).filter(e -> {
            for (Double a : e) {
                if (a.intValue() == 0) {
                    return false;
                }
            }
            return true;
        }).count());
        for (int i = 0; i < rank; i++) {
            generatingSet.removeLastRow();
        }
        return Lattice.from(generatingSet);
    }

    /**
     * Returns the rank of the lattice, i.e. the number if basis vectors.
     */
    public int rank() {
        return this.basis.rows();
    }

    /**
     * Returns the dimension of the ambient space.
     */
    public int ambientDim() {
        return this.basis.columns();
    }

    /**
     * Samples a point from the lattice that is added to the initial vector.
     */
    public Vector samplePointImpl(int bits, Vector initial) {
        assert !this.isEmpty() : "Lattice is empty.";
        assert initial.length() == this.ambientDim();

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // yeah, we need this
        final Vector[] s = new Vector[]{initial};

        IntStream.range(0, this.basis.rows()).mapToObj(this.basis::getRow).forEach(b -> {
//            long f = new BigInteger(bits, ThreadLocalRandom.current()).longValue();

            // this will cause overflow
//            s[0] = s[0].add(b.multiply(f));
            s[0] = VectorHelper.mulRandomAndAdd(s[0], b, bits);
        });

        return s[0];
    }
}
