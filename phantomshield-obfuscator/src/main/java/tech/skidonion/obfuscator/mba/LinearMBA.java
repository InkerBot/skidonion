package tech.skidonion.obfuscator.mba;

import org.la4j.Matrix;
import org.la4j.Vector;
import tech.skidonion.obfuscator.utils.commons.Pair;
import tech.skidonion.obfuscator.utils.commons.Subs;
import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.expr.operations.model.DoubleExprOp;
import tech.skidonion.obfuscator.mba.expr.uniformexpr.LUExpr;
import tech.skidonion.obfuscator.mba.expr.uniformexpr.UExpr;
import tech.skidonion.obfuscator.mba.helper.BigIntHelper;
import tech.skidonion.obfuscator.mba.helper.Valuation;
import tech.skidonion.obfuscator.mba.obfuscate.ObfuscationConfig;
import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.FiniteFail;
import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.FiniteOriginal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class LinearMBA {


    /**
     * Rewrite a linear combination of uniform expression
     * using a set of uniform expressions modulo `2^bits`.
     */
    private static Optional<LUExpr> rewrite(LUExpr expr, ArrayList<LUExpr> ops, int bits, boolean randomize) {
        // Find all variables we have access to.
        // This includes variables in the expression as well as potentially the ops.
        LinkedHashSet<String> _v = new LinkedHashSet<>();
        expr.varsImpl(_v);

        ops.forEach(e -> e.varsImpl(_v));

        ArrayList<String> v = new ArrayList<>(_v);

        // Solve the system.
        AffineLattice l = solveLinearSystem(expr, ops, v, bits);

        // Does it have solutions?
        if (l.isEmpty()) {
            return Optional.empty();
        }

        // Sample a point from the lattice.
        Vector solution;
        if (randomize) {
            solution = l.samplePoint(bits);
        } else {
            solution = l.getOffset();
        }
        // Collect the solution into an LUExpr.
        return Optional.of(collectSolution(solution, ops, bits));
    }

    /**
     * Obfuscate any expression using linear MBA.
     */
    public static void obfuscate(Expr e, int bits, ObfuscationConfig cfg) {
        // Find all variables we have access to.
        ArrayList<String> vars = e.getOp().vars();

        IntStream.range(0, cfg.getRewriteVars() - vars.size()).forEach(i -> vars.add(String.format("aux%s", i)));
        HashSet<ExprOp> v = new HashSet<>();
        obfuscateImpl(e, v, vars, bits, cfg);
    }

    private static LUExpr rewriteRandom(LUExpr e, ArrayList<String> vars, int bits, ObfuscationConfig cfg) {

        for (String v : e.vars()) {
            if (!vars.contains(v)) {
                vars.add(v);
            }
        }

        switch (cfg.getRewriteTries().type()) {
            case Infinite: {
                for (; ; ) {
                    Optional<LUExpr> opt = tryRewrite(e, vars, bits, cfg);
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                }
            }
            case FiniteFail: {
                FiniteFail rewriteTries = (FiniteFail) cfg.getRewriteTries();
                for (int i = 0; i < rewriteTries.triesTimes(); i++) {
                    Optional<LUExpr> opt = tryRewrite(e, vars, bits, cfg);
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                }
                throw new RuntimeException("Failed to rewrite uniform expression.");
            }
            case FiniteOriginal: {
                FiniteOriginal rewriteTries = (FiniteOriginal) cfg.getRewriteTries();
                for (int i = 0; i < rewriteTries.triesTimes(); i++) {
                    Optional<LUExpr> opt = tryRewrite(e, vars, bits, cfg);
                    if (opt.isPresent()) {
                        return opt.get();
                    }
                }
                return e;
            }
            default:
                throw new RuntimeException("unreachable code");
        }
    }

    private static Optional<LUExpr> tryRewrite(LUExpr e, ArrayList<String> vars, int bits, ObfuscationConfig cfg) {
        ArrayList<LUExpr> ops = new ArrayList<>();
        ops.add(LUExpr.from(UExpr.ones()));
        IntStream.range(0, cfg.getRewriteExprCount())
                .forEach(i -> ops.add(LUExpr.from(randomBoolExpr(vars, cfg.getRewriteExprDepth()))));
        return rewrite(e, ops, bits, true);
    }

    private static void obfuscateImpl(Expr er, HashSet<ExprOp> visited, ArrayList<String> vars, int bits, ObfuscationConfig cfg) {

        // Check if we have already visited this expression.
        if (visited.contains(er.getOp())) {
            return;
        }
        visited.add(er.getOp());

        ExprOp e = er.getOp();

        // Try to find the largest subexpression that is
        // linear MBA and obfuscate it on its own.

        Optional<Pair<LUExpr, Subs>> luExprSubsPair = exprToLUExpr(er, false);

        if (luExprSubsPair.isPresent()) {
            Pair<LUExpr, Subs> pair = luExprSubsPair.get();
            LUExpr lu = pair.getFirst();
            Subs subs = pair.getSecond();
            er.setOp(rewriteRandom(lu, vars, bits, cfg).toExpr().getOp());

            for (Pair<String, Expr> _pair : subs.getSubs()) {
                String var = _pair.getFirst();
                Expr sub = _pair.getSecond();
                obfuscateImpl(sub, visited, vars, bits, cfg);
                er.substitute(var, sub);
            }
            return;
        }

        // If the expression isn't linear MBA, recurse on the subexpressions.
        switch (e.type()) {
            case Mul:
            case Div:
            case Shl:
            case Shr:
            case Sar:
                DoubleExprOp m = (DoubleExprOp) e;
                obfuscateImpl(m.getLeft(), visited, vars, bits, cfg);
                obfuscateImpl(m.getRight(), visited, vars, bits, cfg);
            default:
                throw new RuntimeException(String.format("Expression should be linear MBA, but expr_to_luexpr failed (%s)", e));
        }
    }

    /**
     * Creates the linear system corresponding to the target expression
     * and rewrite operations.
     */
    private static AffineLattice solveLinearSystem(LUExpr expr, ArrayList<LUExpr> ops, ArrayList<String> vars, int bits) {
        // If you want more than 32 vars, get a lot of RAM
        // and change the iterators to use Integer instead of usize.
        assert vars.size() < 32 : "More than 31 variables are currently not supported (You wouldn't be able to run this anyways).";
        assert bits < 33 : "Oops! We don't make a big integer matrix & vector arithmetic! So it only 32 bits supported.";
        // Allocate a valuation.
        Valuation val = Valuation.zero();
        int rows = 1 << vars.size();
        int cols = ops.size();

        Matrix a = Matrix.zero(rows, cols);
        Vector b = Vector.zero(rows);
        // Build up the matrix.
        for (int i = 0; i < rows; i++) {
            Vector row = a.getRow(i);
            // Initialize the valuation.

            for (int j = 0; j < vars.size(); j++) {
                String c = vars.get(j);
                val.setValue(c, BigInteger.valueOf((i >> j) & 1).negate());
            }
            // Write the values of the operations into this row of the matrix.
            for (int j = 0; j < ops.size(); j++) {
                LUExpr e = ops.get(j);
                row.set(j, (double) BigIntHelper.keepSignedBits(e.eval(val, bits), bits).longValue());
            }
            a.setRow(i, row);
            // Write the desired result into the vector.
            b.set(i, (double) BigIntHelper.keepSignedBits(expr.eval(val, bits), bits).longValue());
        }
        return Diophantine.solveModular(a, b, 1L << bits);
    }

    /**
     * Converts a solution to the linear system and the operations
     * to a single `LUExpr` with no duplicate `UExpr`s.
     */
    private static LUExpr collectSolution(Vector solution, ArrayList<LUExpr> ops, int bits) {
        // Put it in a LUExpr.
        LUExpr l = LUExpr.zero();
        if (solution.length() != ops.size())
            throw new IllegalArgumentException("solution length does not match op length");

        for (int i = 0; i < solution.length(); i++) {
            double c = solution.get(i);
            LUExpr o = ops.get(i);
            for (Pair<BigInteger, UExpr> datum : o.getData()) {
                // Is the UExpr already in the linear combination?
                BigInteger d = datum.getFirst();
                UExpr e = datum.getSecond();
                Optional<Pair<BigInteger, UExpr>> opt = l.getData().stream().filter(p -> p.getSecond() == e).findFirst();
                if (opt.isPresent()) {
                    Pair<BigInteger, UExpr> pair = opt.get();
                    BigInteger f = pair.getFirst();
                    pair.setFirst(f.add(BigInteger.valueOf((long) c).multiply(d)));
                } else {
                    l.getData().add(new Pair<>(BigInteger.valueOf((long) c).multiply(d), e));
                }
            }
        }
        for (Pair<BigInteger, UExpr> datum : l.getData()) {
            BigInteger i = datum.getFirst();
            datum.setFirst(BigIntHelper.keepSignedBits(i, bits));
        }

        l.removeZeroTerms();
        return l;
    }

    /**
     * Converts part of an expression to a UExpr,
     * such that if you substituted the Exprs in `subs` for the variables,
     * you would get the original Expr.
     * It will generally try to make the LUExpr as big as possible.
     * If `force` is false, it will return [None] if the top-most operation
     * is not a [UExpr] operation.
     * Otherwise, it will return a [UExpr::Var] whose substitution
     * is the original expression.
     */
    private static Optional<UExpr> exprToUExpr(Expr e, Subs subs, boolean force) {
        // New substitution variable.
        Supplier<Optional<UExpr>> new_sub = () -> {
            if (force) {
                return Optional.of(UExpr.var(subs.add(e)));
            } else {
                return Optional.empty();
            }
        };
        if (e.getOp().type() == ExprOp.ExprOpType.Var) {
            return Optional.of(UExpr.var(((Var) e.getOp()).getVar()));
        }

//        // We don't try, when the expression is shared.
//        if (e.getParent() != null) {
//            return new_sub.get();
//        }

        switch (e.getOp().type()) {
            case And: {
                And m = ((And) e.getOp());
                return Optional.of(UExpr.and(exprToUExpr(m.getLeft(), subs, true).get(), exprToUExpr(m.getRight(), subs, true).get()));
            }
            case Or: {
                Or m = ((Or) e.getOp());
                return Optional.of(UExpr.or(exprToUExpr(m.getLeft(), subs, true).get(), exprToUExpr(m.getRight(), subs, true).get()));
            }
            case Xor: {
                Xor m = ((Xor) e.getOp());
                return Optional.of(UExpr.xor(exprToUExpr(m.getLeft(), subs, true).get(), exprToUExpr(m.getRight(), subs, true).get()));
            }
            case Not: {
                Not m = ((Not) e.getOp());
                return Optional.of(UExpr.not(exprToUExpr(m.getExpr(), subs, true).get()));
            }
            default:
                // Otherwise generate a new variable and add the substitution.
                return new_sub.get();
        }
    }

    /**
     * Tries to convert an expression into a factor and a UExpr.
     */
    private static Optional<Pair<BigInteger, UExpr>> parseTerm(Expr e, Subs subs, boolean force) {
        if (e.getOp().type() == ExprOp.ExprOpType.Mul) {
            Mul m = ((Mul) e.getOp());
            Expr l = m.getLeft();
            Expr r = m.getRight();
            if (l.getOp().type() == ExprOp.ExprOpType.Const) {
                return exprToUExpr(r, subs, force).map(u -> new Pair<>(new BigInteger(((Const) l.getOp()).getVal().toByteArray()), u));
            } else if (r.getOp().type() == ExprOp.ExprOpType.Const) {
                return exprToUExpr(l, subs, force).map(u -> new Pair<>(new BigInteger(((Const) l.getOp()).getVal().toByteArray()), u));
            }
        } else if (e.getOp().type() == ExprOp.ExprOpType.Const) {
            return Optional.of(new Pair<>(((Const) e.getOp()).getVal().negate(), UExpr.ones()));
        }

        return exprToUExpr(e, subs, force).map(u -> new Pair<>(BigInteger.ONE, u));
    }

    /**
     * Converts part of an expression to an [LUExpr].
     * Returns the [LUExpr] and a list of substitutions,
     * such that substituting the expressions in the list
     * into the variables, would give the original expression.
     * It will generally try to make the LUExpr as big as possible.
     * If `force` is false, it will return [None] if the top-most operation
     * is not something a [LUExpr] can represent (e.g. [Expr::Div]).
     * Otherwise, it will return a new variable, whose substitution
     * is the original expression.
     */
    private static Optional<Pair<LUExpr, Subs>> exprToLUExpr(Expr e, boolean force) {
        LUExpr lu = LUExpr.zero();
        Subs subs = new Subs();
        if (exprToLUExprImpl(e, lu, subs, false, force)) {
            return Optional.of(new Pair<>(lu, subs));
        } else {
            return Optional.empty();
        }
    }

    private static boolean exprToLUExprImpl(Expr e, LUExpr lu, Subs subs, boolean negate, boolean force) {
        switch (e.getOp().type()) {
            case Add: {
                Add m = (Add) e.getOp();
                exprToLUExprImpl(m.getLeft(), lu, subs, negate, true);
                exprToLUExprImpl(m.getRight(), lu, subs, negate, true);
                return true;
            }
            case Sub: {
                Sub m = (Sub) e.getOp();
                exprToLUExprImpl(m.getLeft(), lu, subs, negate, true);
                exprToLUExprImpl(m.getRight(), lu, subs, !negate, true);
                return true;
            }
            case Neg: {
                Neg m = ((Neg) e.getOp());
                int c = negate ? 1 : -1;
                // Theoretically we could allow another whole
                // LUExpr in here but hopefully not too important.
                lu.getData().add(new Pair<>(BigInteger.valueOf(c), exprToUExpr(m.getExpr(), subs, true).get()));
                return true;
            }
            // Otherwise parse the term from this expression.
            default: {
                Optional<Pair<BigInteger, UExpr>> p = parseTerm(e, subs, force);
                BigInteger f;
                UExpr u;
                if (p.isPresent()) {
                    Pair<BigInteger, UExpr> pair = p.get();
                    f = pair.getFirst();
                    u = pair.getSecond();
                } else {
                    return false;
                }

                if (negate) {
                    f = f.negate();
                }
                lu.getData().add(new Pair<>(f, u));
                return true;
            }
        }
    }

    /**
     * Generates a random boolean expression.
     * It would be very desirable to make this smarter.
     * Currently it generates a lot of non-sense expressions,
     * which simplify to zero or one easily.
     */
    private static UExpr randomBoolExpr(ArrayList<String> vars, int maxDepth) {
        assert !vars.isEmpty() : "There needs to be at least one variable for the random expression.";

        Supplier<UExpr> rand_var = () -> UExpr.var(vars.get(ThreadLocalRandom.current().nextInt(vars.size())));
        if (maxDepth == 0) {
            return rand_var.get();
        }

        // Generate one of the four variants uniformly at random.
        int d = maxDepth - 1;
        switch (ThreadLocalRandom.current().nextInt(5)) {
            case 0:
                return rand_var.get();
            case 1:
                return UExpr.not(randomBoolExpr(vars, d));
            case 2:
                return UExpr.and(randomBoolExpr(vars, d), randomBoolExpr(vars, d));
            case 3:
                return UExpr.or(randomBoolExpr(vars, d), randomBoolExpr(vars, d));
            case 4:
                return UExpr.xor(randomBoolExpr(vars, d), randomBoolExpr(vars, d));
            default:
                throw new RuntimeException("unreachable code");
        }
    }
}
