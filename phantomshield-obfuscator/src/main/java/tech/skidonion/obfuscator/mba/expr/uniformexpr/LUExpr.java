package tech.skidonion.obfuscator.mba.expr.uniformexpr;

import tech.skidonion.obfuscator.utils.commons.Pair;
import tech.skidonion.obfuscator.utils.commons.PeekableCharacterIterator;
import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;
import tech.skidonion.obfuscator.mba.helper.BigIntHelper;
import tech.skidonion.obfuscator.mba.helper.Valuation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LUExpr {
    private final ArrayList<Pair<BigInteger, UExpr>> data;

    public ArrayList<Pair<BigInteger, UExpr>> getData() {
        return data;
    }

    public LUExpr() {
        data = new ArrayList<>();
    }

    public LUExpr(ArrayList<Pair<BigInteger, UExpr>> data) {
        this.data = data;
    }

    public static LUExpr from(UExpr expr) {
        ArrayList<Pair<BigInteger, UExpr>> data = new ArrayList<>();
        data.add(new Pair<>(BigInteger.ONE, expr));
        return new LUExpr(data);
    }


    /**
     * Create the empty linear combination.
     * It evaluates to 0.
     */
    public static LUExpr zero() {
        return new LUExpr(new ArrayList<>());
    }


    /**
     * Creates an expression that equals a constant.
     */
    public static LUExpr constant(BigInteger val) {
        ArrayList<Pair<BigInteger, UExpr>> data = new ArrayList<>();
        data.add(new Pair<>(val.negate(), UExpr.ones()));
        return new LUExpr(data);
    }

    /**
     * Creates an expression that equals a variable.
     */
    public static LUExpr var(String name) {
        ArrayList<Pair<BigInteger, UExpr>> data = new ArrayList<>();
        data.add(new Pair<>(BigInteger.ONE, UExpr.var(name)));
        return new LUExpr(data);
    }

    /**
     * Removes all terms with coefficient 0.
     */
    public void removeZeroTerms() {
        this.data.removeIf(p -> BigInteger.ZERO.equals(p.getFirst()));
    }

    /**
     * Returns all variables in the expression.
     * This will include duplicates.
     */
    public ArrayList<String> vars() {
        LinkedHashSet<String> v = new LinkedHashSet<>();
        this.varsImpl(v);
        return new ArrayList<>(v);
    }

    public void varsImpl(LinkedHashSet<String> v) {
        for (Pair<BigInteger, UExpr> pair : data) {
            pair.getSecond().varsImpl(v);
        }
    }

    /**
     * Evaluate an expression with a valuation for the occurring variables.
     */
    public BigInteger eval(Valuation v, int bits) {
        return this.data.stream().map(p -> p.getFirst().multiply(p.getSecond().eval(v))).reduce(BigInteger.ZERO, (acc, x) -> BigIntHelper.keepBits(acc.add(x), bits));
    }

    /**
     * Returns a measure of the complexity of the expression.
     */
    public int complexity(int bits) {
        // Complexity of a coefficient.
        Function<BigInteger, Integer> coeff_complexity = (BigInteger i) -> {
            BigInteger signed = BigIntHelper.keepSignedBits(i, bits);
            BigInteger abs = signed.abs();
            int count = 0;
            for (int j = 0; j < abs.bitLength(); j++) {
                if (i.testBit(j)) {
                    count++;
                }
            }

            return count / 2 + abs.bitLength();
        };
        return this.data.stream().filter(p -> !BigInteger.ZERO.equals(p.getFirst())).mapToInt(p -> coeff_complexity.apply(p.getFirst()) + p.getSecond().complexity()).sum();
    }

    /**
     * Converts an LUExpr to an Expr.
     */
    public Expr toExpr() {
        Iterator<Pair<BigInteger, UExpr>> it = this.data.iterator();
        Pair<BigInteger, UExpr> s;
        if (it.hasNext()) {
            s = it.next();
        } else {
            return ExprOp.constant(BigInteger.ZERO);
        }

        Function<Pair<BigInteger, UExpr>, Expr> from_summand = (Pair<BigInteger, UExpr> p) -> {
            if (p.getSecond().type() == UExpr.UExprType.Ones) {
                return ExprOp.constant(new BigInteger(p.getFirst().toByteArray()).negate());
            } else {
                return ExprOp.mul(ExprOp.constant(new BigInteger(p.getFirst().toByteArray())), p.getSecond().toExpr());
            }
        };

        Expr cur = from_summand.apply(s);

        while (it.hasNext()) {
            s = it.next();
            cur = ExprOp.add(cur, from_summand.apply(s));
        }

        return cur;
    }

    /**
     * Parse a string to an expression.
     * Note that this function is extremely limited
     * and expects very specific syntax.
     * It is used for convenience when testing things and
     * not really meant to be used by something outside this crate.
     */
    public static Optional<LUExpr> fromString(String s) {
        s = s.replaceAll("\\s+", "");

        PeekableCharacterIterator it = new PeekableCharacterIterator(s);

        // This stores the current linear combination.
        ArrayList<Pair<BigInteger, UExpr>> v = new ArrayList<>();
        boolean neg = false;

        // Loop over the string/the summands.
        for (; ; ) {

            // Are there still characters left?
            // If not then we're done.
            Character c;
            if (it.hasNext()) {
                c = it.peek();
            } else {
                return Optional.of(new LUExpr(v));
            }
            if (c == '-') {
                neg = true;
                it.next();
                c = it.peek();
                if (c == null) return Optional.empty();
            }
            // If this is a digit then we expect num*UExpr.
            if (Character.isDigit(c)) {

                // Parse the number.
                StringBuilder sb = new StringBuilder();
                do {
                    sb.append(it.next());
                } while (it.hasNext() && Character.isDigit(it.peek()));
                BigInteger num = new BigInteger(sb.toString());

                // If the number is negative then negate it.
                if (neg) {
                    num = num.negate();
                }

                // Is it the expected '*'?
                Character lookahead = it.peek();

                if (lookahead != null && lookahead == '*') {
                    it.peek();
                    // Parse the UExpr.
                    Optional<UExpr> e = UExpr.parse(it, 0);
                    if (!e.isPresent()) return Optional.empty();
                    // Push it.
                    v.add(new Pair<>(num, e.get()));
                } else {
                    // If this is a different character then we push -num*(-1).
                    v.add(new Pair<>(num.negate(), UExpr.ones()));
                }
            } else {
                // We don't have a factor so just parse the UExpr.
                Optional<UExpr> e = UExpr.parse(it, 0);
                if (!e.isPresent()) return Optional.empty();
                int sign = neg ? -1 : 1;

                // Push sign*e.
                v.add(new Pair<>(BigInteger.valueOf(sign), e.get()));
            }
            // If the next character is not a plus or - then we are done.

            Character _c = it.peek();
            if (_c != null) {
                if (_c == '+') {
                    neg = false;
                    it.next();
                } else if (_c == '-') {
                    neg = true;
                    it.next();
                }
            } else {
                return Optional.of(new LUExpr(v));
            }
        }
    }

    @Override
    public String toString() {
        Iterator<Pair<BigInteger, UExpr>> iter = this.data.stream().filter(p -> !BigInteger.ZERO.equals(p.getFirst())).collect(Collectors.toList()).iterator();

        BigInteger i;
        UExpr e;
        if (iter.hasNext()) {
            Pair<BigInteger, UExpr> next = iter.next();
            i = next.getFirst();
            e = next.getSecond();
        } else {
            return "0";
        }
        StringBuilder sb = new StringBuilder();
        if (BigInteger.ONE.equals(i)) {
            sb.append(BigInteger.ONE);
        } else {
            sb.append(String.format("%s*(%s)", i, e));
        }
        while (iter.hasNext()) {
            Pair<BigInteger, UExpr> next = iter.next();
            i = next.getFirst();
            e = next.getSecond();
            sb.append(" + ");
            if (BigInteger.ONE.equals(i)) {
                sb.append(e);
            } else {
                sb.append(String.format("%s*(%s)", i, e));
            }
        }
        return sb.toString();
    }
}
