package tech.skidonion.obfuscator.mba;


import org.la4j.Matrix;
import org.la4j.Vector;
import tech.skidonion.obfuscator.mba.helper.MathHelper;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Diophantine {

    /**
     * Computes the (row-style) hermite normal form of a matrix in place
     * and returns the transformation matrix.
     */
    public static Matrix hermiteNormalForm(final Matrix a) {
        // The transformation matrix.
        final Matrix u = Matrix.identity(a.rows());
        int r = 0;
        int c = 0;

        while (r < a.rows() && c < a.columns()) {

            // Choose a pivot in the jth column.

            Vector column = a.getColumn(c);
            Optional<Integer> pivotOpt = IntStream.range(r, column.length()).filter(i -> (long) column.get(i) != 0).boxed().min(Comparator.comparingDouble(i -> Math.abs(column.get(i))));

            if (!pivotOpt.isPresent()) {
                // If we didn't find a pivot then the column is 0.
                // Continue with the next one.
                c++;
                continue;
            }
            int pivot = pivotOpt.get();
            // Move the pivot to the beginning.
            a.swapRows(r, pivot);
            u.swapRows(r, pivot);

            // Try to eliminate every other entry in the column.
            // This might not work instantly.
            // If there remain non-zero entries in this column,
            // then we will go over this column again.
            final int _c = c;
            final int _r = r;
            IntStream.range(r + 1, a.rows()).forEach(k -> {
                if ((int) a.get(k, _c) != 0) {
                    long m = -(long) (a.get(k, _c) / a.get(_r, _c));
                    for (int i = 0; i < a.columns(); i++) {
                        long tmp = (long) a.get(_r, i) * m;
                        a.set(k, i, (double) ((long) a.get(k, i) + tmp));
                    }
                    for (int i = 0; i < u.columns(); i++) {
                        long tmp = (long) u.get(_r, i) * m;
                        u.set(k, i, (double) ((long) u.get(k, i) + tmp));
                    }
                }
            });

            // If there is any non-zero element then we need to continue in the same column.
            if (IntStream.range(0, column.length()).skip(r + 1).anyMatch(i -> (long) column.get(i) != 0)) {
                continue;
            }

            // Flip sign if necessary.
            if (a.get(r, c) < 0.0D) {
                Vector row;
                row = a.getRow(r);
                for (int i = 0; i < row.length(); i++) {
                    row.set(i, -(long) row.get(i));
                }
                row = u.getRow(r);
                for (int i = 0; i < row.length(); i++) {
                    row.set(i, -(long) row.get(i));
                }
            }
            // Reduce the elements above the pivot
            // (in the column of the pivot and rows above the pivot).
            // The Hermite normal form requires the entries
            // above the pivot to be positive.
            if ((long) a.get(r, c) != 0) {
                IntStream.range(0, r).forEach(k -> {
                    long entry = (long) a.get(k, _c);

                    long m = MathHelper.divEuclid(entry, (long) a.get(_r, _c));
                    if (m != 0) {
                        for (int i = 0; i < a.columns(); i++) {
                            long tmp = (long) a.get(_r, i) * m;
                            a.set(k, i, (double) ((long) a.get(k, i) + tmp));
                        }
                        for (int i = 0; i < u.columns(); i++) {
                            long tmp = (long) u.get(_r, i) * m;
                            u.set(k, i, (double) ((long) u.get(k, i) + tmp));
                        }
                    }
                });
            }
            // Continue with the bottom right part of the matrix that remains.
            c += 1;
            r += 1;
        }

        return u;
    }

    /**
     * Solves a system of linear diophantine equations.
     */
    public static AffineLattice solve(Matrix a, Vector b) {
        assert a.rows() == b.length() : "Vector must have an entry for each row in the matrix.";
        Matrix m = Matrix.zero(a.columns() + 1, a.rows() + 1);

        // Initialize the matrix m.

        for (int i = 0; i < a.rows(); i++) {
            Vector row = a.getRow(i);
            for (int j = 0; j < row.length(); j++) {
                m.set(j, i, row.get(j));
            }
        }
        for (int i = 0; i < b.length(); i++) {
            m.set(a.columns(), i, b.get(i));
        }
        m.set(a.columns(), a.rows(), 1.0D);

        // Transform it into hermite normal form.
        Matrix u = hermiteNormalForm(m);
        // Compute the rank of the matrix.
        // It has a special form that we can take advantage of.
        int rank = (int) IntStream.range(0, m.rows()).mapToObj(m::getRow).filter(d -> {
            for (Double e : d) {
                if (e.intValue() != 0) {
                    return true;
                }
            }
            return false;
        }).count();
        // Make sure the hermite normal form has the correct form,
        // because only then does it have a solution.
        int r = rank - 1;
        boolean has_solution = (long) m.get(r, m.columns() - 1) == 1 && IntStream.range(0, m.getRow(r).length()).limit(m.columns() - 1).allMatch(i -> {
            long val = (long) m.getRow(r).get(i);
            return val == 0;
        });
        if (!has_solution) {
            return AffineLattice.empty();
        }
        Vector offset;
        {
            Vector row = u.getRow(r);
            offset = Vector.fromCollection(IntStream.range(0, u.rows() - 1).mapToObj(row::get).collect(Collectors.toList()));
            for (int i = 0; i < offset.length(); i++) {
                offset.set(i, -(long) offset.get(i));
            }
        }
        Matrix basis;
        {
            basis = Matrix.from1DArray(u.rows() - rank, u.rows() - 1, IntStream.range(0, u.rows()).skip(rank).mapToObj(u::getRow).flatMapToDouble(row -> IntStream.range(0, u.rows() - 1).mapToDouble(row::get)).toArray());
        }
        return AffineLattice.from(offset, basis);
    }

    /**
     * Solves a linear system of equations Ax=b mod n.
     * The solution lattice consists of all integer solutions to the equations.
     */
    public static AffineLattice solveModular(Matrix a, Vector b, long n) {
        //
        // Concatenate an n times the identity matrix to the right of A.
        //
        Matrix m = Matrix.zero(a.rows(), a.columns() + a.rows());
        // Copy the old matrix.
        for (int i = 0; i < a.rows(); i++) {
            for (int j = 0; j < a.columns(); j++) {
                m.set(i, j, a.get(i, j));
            }
        }

        // Set the identity matrix.
        for (int i = 0; i < a.rows(); i++) {
            m.set(i, a.columns() + i, n);
        }
        // Solve the diophantine system.
        AffineLattice l = solve(m, b);
        if (l.isEmpty()) {
            return l;
        }

        // Clean up the solution by taking everything mod n
        // removing the last components that correspond to the multipliers
        // of the n's and then removing (now) linearly dependent basis vectors.

        Vector offset;
        {
            Vector row = l.getOffset();
            offset = Vector.fromArray(IntStream.range(0, a.columns()).mapToDouble(row::get).map(i -> (double) MathHelper.remEuclid((long) i, n)).toArray());
        }
        // This might be the worst code in the history of code.

        Stream<Double> iter;
        {
            Matrix basis = l.getLattice().getBasis();
            iter = Stream.concat(
                    IntStream.range(0, basis.rows()).mapToObj(basis::getRow)
                            .flatMapToDouble(e -> IntStream.range(0, e.length())
                                    .mapToDouble(e::get).limit(a.columns()).map(i -> (double) ((long) i) % n)).boxed(),
                    IntStream.range(0, a.columns()).mapToDouble(i -> (double) i)
                            .flatMap(i -> IntStream.range(0, a.columns())
                                    .mapToDouble(j -> (double) j).map(j -> ((long) i == (long) j) ? (double) n : 0.0D)).boxed()
            );
        }
        Matrix bm = Matrix.from1DArray(l.getLattice().rank() + a.columns(), a.columns(), iter.mapToDouble(Double::doubleValue).toArray());

        Lattice lattice = Lattice.fromGeneratingSet(bm);
        return new AffineLattice(offset, lattice);
    }
}
