package tech.skidonion.obfuscator.mba;

import org.la4j.Matrix;
import org.la4j.Vector;

public class AffineLattice {
    private final Vector offset;
    private final Lattice lattice;

    public Lattice getLattice() {
        return lattice;
    }

    public Vector getOffset() {
        return offset;
    }

    public AffineLattice(Vector offset, Lattice lattice) {
        this.offset = offset;
        this.lattice = lattice;
    }

    /**
     * Creates an empty lattice.
     */
    public static AffineLattice empty() {
        return new AffineLattice(Vector.zero(0), Lattice.empty());
    }

    /**
     * Creates an affine lattice from an offset and a basis.
     */
    public static AffineLattice from(Vector offset, Matrix basics) {
        return new AffineLattice(offset, Lattice.from(basics));
    }

    /**
     * Is this lattice empty?
     */
    public boolean isEmpty() {
        return offset.length() == 0;
    }

    /**
     * Returns a random point on the lattice mod 2^bits.
     */
    public Vector samplePoint(int bits) {
        return this.lattice.samplePointImpl(bits, this.offset.copy());
    }
}
