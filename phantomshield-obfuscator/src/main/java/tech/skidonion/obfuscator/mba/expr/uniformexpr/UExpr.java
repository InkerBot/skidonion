package tech.skidonion.obfuscator.mba.expr.uniformexpr;

import tech.skidonion.obfuscator.utils.commons.PeekableCharacterIterator;
import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;
import tech.skidonion.obfuscator.mba.expr.uniformexpr.models.*;
import tech.skidonion.obfuscator.mba.helper.Valuation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Optional;

public abstract class UExpr {
    public enum UExprType {
        Ones, Var, And, Or, Xor, Not;
    }

    public abstract UExprType type();

    public static Ones ones() {
        return Ones.ONES;
    }

    public static Var var(String symbol) {
        return new Var(symbol);
    }

    public static And and(UExpr left, UExpr right) {
        return new And(left, right);
    }

    public static Or or(UExpr left, UExpr right) {
        return new Or(left, right);
    }

    public static Xor xor(UExpr left, UExpr right) {
        return new Xor(left, right);
    }

    public static Not not(UExpr expr) {
        return new Not(expr);
    }

    /**
     * Returns all variables in the expression.
     * This will include duplicates.
     */
    public ArrayList<String> vars() {
        LinkedHashSet<String> v = new LinkedHashSet<>();
        varsImpl(v);
        return new ArrayList<>(v);
    }

    void varsImpl(LinkedHashSet<String> v) {
        switch (this.type()) {
            case Ones:
                break;
            case Var:
                v.add(((Var) this).getSymbol());
                break;
            case And: {
                And e = ((And) this);
                e.getLeft().varsImpl(v);
                e.getRight().varsImpl(v);
                break;
            }
            case Or: {
                Or e = ((Or) this);
                e.getLeft().varsImpl(v);
                e.getRight().varsImpl(v);
                break;
            }
            case Xor: {
                Xor e = ((Xor) this);
                e.getLeft().varsImpl(v);
                e.getRight().varsImpl(v);
                break;
            }
            case Not: {
                Not e = ((Not) this);
                e.getExpr().varsImpl(v);
                break;
            }
        }
    }

    /**
     * Evaluate an expression with a valuation for the occurring variables.
     */
    public BigInteger eval(Valuation v) {
        switch (this.type()) {
            case Ones:
                return BigInteger.valueOf(-1);
            case Var:
                return v.value(((Var) this).getSymbol());
            case And: {
                And m = ((And) this);
                return m.getLeft().eval(v).and(m.getRight().eval(v));
            }
            case Or: {
                Or m = ((Or) this);
                return m.getLeft().eval(v).or(m.getRight().eval(v));
            }
            case Xor: {
                Xor m = ((Xor) this);
                return m.getLeft().eval(v).xor(m.getRight().eval(v));
            }
            case Not: {
                Not m = ((Not) this);
                return m.getExpr().eval(v).not();
            }
            default:
                throw new RuntimeException("Unhandled type: " + this.type());
        }
    }

    /**
     * Rename a variable.
     */
    public void renameVar(String older, String newer) {
        switch (this.type()) {
            case Ones:
                break;
            case Var: {
                Var var = ((Var) this);
                if (older.equals(var.getSymbol())) {
                    var.setSymbol(newer);
                }
                break;
            }
            case And: {
                And e = ((And) this);
                e.getLeft().renameVar(older, newer);
                e.getRight().renameVar(older, newer);
                break;
            }
            case Or: {
                Or e = ((Or) this);
                e.getLeft().renameVar(older, newer);
                e.getRight().renameVar(older, newer);
                break;
            }
            case Xor: {
                Xor e = ((Xor) this);
                e.getLeft().renameVar(older, newer);
                e.getRight().renameVar(older, newer);
                break;
            }
            case Not: {
                Not e = ((Not) this);
                e.getExpr().renameVar(older, newer);
            }
        }
    }

    /**
     * Returns some sort of complexity measure of the expression.
     */
    public int complexity() {
        switch (this.type()) {
            case Ones:
            case Var:
                return 1;
            case And: {
                And e = ((And) this);
                return e.getLeft().complexity() + e.getRight().complexity() + 1;
            }
            case Or: {
                Or e = ((Or) this);
                return e.getLeft().complexity() + e.getRight().complexity() + 1;
            }
            case Xor: {
                Xor e = ((Xor) this);
                return e.getLeft().complexity() + e.getRight().complexity() + 1;
            }
            case Not: {
                Not e = ((Not) this);
                return e.getExpr().complexity() + e.getExpr().complexity() + 1;
            }
            default:
                throw new RuntimeException("Unhandled type: " + this.type());
        }
    }

    private static String writeSafe(UExpr e1, UExpr e2, char op) {
        StringBuilder sb = new StringBuilder();
        if (e1.type() == UExprType.Var) {
            Var var = (Var) e1;
            sb.append(String.format("%s %s", var.getSymbol(), op));
        } else {
            sb.append(String.format("(%s) %s", e1, op));
        }

        if (e2.type() == UExprType.Var) {
            Var var = (Var) e2;
            sb.append(String.format(" %s", var.getSymbol()));
        } else {
            sb.append(String.format(" (%s)", e2));
        }
        return sb.toString();
    }

    /**
     * Parse a string to an expression.
     */
    public static Optional<UExpr> fromString(String s) {
        return parse(new PeekableCharacterIterator(s.replaceAll("\\s+", "")), 0);
    }


    static Optional<UExpr> parse(PeekableCharacterIterator it, int pre) {
        Character c = it.peek();
        if (c == null) return Optional.empty();
        UExpr e;
        if (c == '(' ) {
            it.next();
            Optional<UExpr> _e = parse(it, 0);
            if (!_e.isPresent()) return Optional.empty();
            if (it.hasNext() && it.next() == ')' ) {
                e = _e.get();
            } else {
                return Optional.empty();
            }
        } else if (c == '~' ) {
            it.next();
            Optional<UExpr> _e = parse(it, 15);
            if (!_e.isPresent()) return Optional.empty();
            e = new Not(_e.get());
        } else if (Character.isAlphabetic(c)) {
            it.next();
            StringBuilder var = new StringBuilder(c.toString());
            for (; ; ) {
                if ((c = it.peek()) == null) break;

                if (!Character.isAlphabetic(c)) break;

                var.append(c);
                it.next();
            }

            e = new Var(var.toString());
        } else if (c == '1' ) {
            it.next();
            e = ones();
        } else {
            return Optional.empty();
        }
        for (; ; ) {
            if (it.hasNext()) {
                c = it.peek();
            } else {
                return Optional.of(e);
            }
            int op_pre;
            switch (c) {
                case '|':
                    op_pre = 1;
                    break;
                case '^':
                    op_pre = 2;
                    break;
                case '&':
                    op_pre = 3;
                    break;
                default:
                    return Optional.of(e);
            }

            if (op_pre <= pre) {
                return Optional.of(e);
            }
            // If the current operators precedence is higher than
            // the one whose subexpressions we are currently parsing
            // then we need to finish this operator first.
            it.next();

            Optional<UExpr> rhs = parse(it, op_pre);
            if (!rhs.isPresent()) return Optional.empty();
            UExpr lhs = e;

            switch (c) {
                case '&':
                    e = new And(lhs, rhs.get());
                    break;
                case '|':
                    e = new Or(lhs, rhs.get());
                    break;
                case '^':
                    e = new Xor(lhs, rhs.get());
                    break;
                default:
                    return Optional.empty();
            }
        }
    }

    /**
     * Converts a UExpr to an Expr.
     */

    public Expr toExpr() {
        switch (this.type()) {
            case Ones:
                return ExprOp.constant(BigInteger.valueOf(-1));
            case Var:
                return ExprOp.var(((Var) this).getSymbol());
            case And: {
                And m = ((And) this);
                return ExprOp.and(m.getLeft().toExpr(), m.getRight().toExpr());
            }
            case Or: {
                Or m = ((Or) this);
                return ExprOp.or(m.getLeft().toExpr(), m.getRight().toExpr());
            }
            case Xor: {
                Xor m = ((Xor) this);
                return ExprOp.xor(m.getLeft().toExpr(), m.getRight().toExpr());
            }
            case Not: {
                Not m = ((Not) this);
                return ExprOp.not(m.getExpr().toExpr());
            }
            default:
                throw new RuntimeException("Unhandled type: " + this.type());
        }
    }


    @Override
    public String toString() {
        switch (this.type()) {
            case Ones:
                return "-1";
            case Var:
                return ((Var) this).getSymbol();
            case And: {
                And m = ((And) this);
                return writeSafe(m.getLeft(), m.getRight(), '&' );
            }
            case Or: {
                Or m = ((Or) this);
                return writeSafe(m.getLeft(), m.getRight(), '|' );
            }
            case Xor: {
                Xor m = ((Xor) this);
                return writeSafe(m.getLeft(), m.getRight(), '^' );
            }
            case Not: {
                Not m = ((Not) this);
                if (m.getExpr().type() == UExprType.Var) {
                    return String.format("~%s", ((Var) m.getExpr()).getSymbol());
                } else {
                    return String.format("~(%s)", m.getExpr());
                }
            }
            default:
                throw new RuntimeException("Unhandled type: " + this.type());
        }
    }
}
