package tech.skidonion.obfuscator.mba.expr;

import tech.skidonion.obfuscator.utils.commons.Triple;
import tech.skidonion.obfuscator.utils.commons.function.Function4;
import tech.skidonion.obfuscator.utils.commons.function.Function3;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.expr.operations.model.DoubleExprOp;
import tech.skidonion.obfuscator.mba.expr.operations.model.SingleExprOp;

import java.math.BigInteger;
import java.util.*;

public abstract class ExprOp {

    private final HashSet<Expr> linked = new HashSet<>();

    /**
     * only use these while referenced by other expr
     */
    public void addLinked(final Expr expr) {
        linked.add(expr);
    }

    public void dropLinked(final Expr expr) {
        linked.remove(expr);
    }

    public void dropAll() {
        linked.clear();
    }

    public int referencedSize() {
        return linked.size();
    }

    public abstract ExprOpType type();

    public static Expr constant(BigInteger val) {
        return new Expr(new Const(val));
    }

    public static Expr var(String var) {
        return new Expr(new Var(var));
    }

    public static Expr add(Expr left, Expr right) {
        return new Expr(new Add(left, right));
    }

    public static Expr sub(Expr left, Expr right) {
        return new Expr(new Sub(left, right));
    }

    public static Expr mul(Expr left, Expr right) {
        return new Expr(new Mul(left, right));
    }

    public static Expr div(Expr left, Expr right) {
        return new Expr(new Div(left, right));
    }

    public static Expr neg(Expr expr) {
        return new Expr(new Neg(expr));
    }

    public static Expr and(Expr left, Expr right) {
        return new Expr(new And(left, right));
    }

    public static Expr or(Expr left, Expr right) {
        return new Expr(new Or(left, right));
    }

    public static Expr xor(Expr left, Expr right) {
        return new Expr(new Xor(left, right));
    }

    public static Expr not(Expr expr) {
        return new Expr(new Not(expr));
    }

    public static Expr shl(Expr left, Expr right) {
        return new Expr(new Shl(left, right));
    }

    public static Expr shr(Expr left, Expr right) {
        return new Expr(new Shr(left, right));
    }

    public static Expr lsh(Expr left, Expr right) {
        return new Expr(new Shl(left, right));
    }

    public final boolean isZero() {
        return this instanceof Const && (((Const) this).getVal()).equals(BigInteger.ZERO);
    }

    public final boolean isOne() {
        return this instanceof Const && (((Const) this).getVal()).equals(BigInteger.ONE);
    }

    public static ExprOp zero() {
        return new Const(BigInteger.ZERO);
    }


    /**
     * Returns all variables in the expression.
     * This can include duplicates.
     */
    public ArrayList<String> vars() {
        LinkedHashSet<String> v = new LinkedHashSet<>();
        vars_impl(v);
        return new ArrayList<>(v);
    }

    public void vars_impl(LinkedHashSet<String> v) {
        switch (this.type()) {
            case Const:
                break;
            case Var:
                v.add(((Var) this).getVar());
                break;
            case Add:
            case Sub:
            case Mul:
            case Div:
            case And:
            case Or:
            case Xor:
            case Shl:
            case Shr:
            case Sar:
                ((DoubleExprOp) this).getLeft().getOp().vars_impl(v);
                ((DoubleExprOp) this).getRight().getOp().vars_impl(v);
                break;
            case Neg:
            case Not:
                ((SingleExprOp) this).getExpr().getOp().vars_impl(v);
                break;
        }
    }

    /**
     * Returns the precedence of a binary operator.
     * All operators are taken to be left associative.
     */
    public int precedence() {
        switch (this.type()) {
            case Or:
                return 1;
            case Xor:
                return 2;
            case And:
                return 3;
            case Shl:
            case Shr:
            case Sar:
                return 4;
            case Add:
            case Sub:
                return 5;
            case Mul:
            case Div:
                return 6;
            case Neg:
            case Not:
                return 15;
            case Const:
            case Var:
                return 16;
            default:
                throw new RuntimeException("unknown operation type: " + this.type());
        }
    }

    private static String printSimpleRc(Expr e, ArrayList<Triple<ExprOp, Character, String>> vars) {
        // If there is only one reference then just print it.
        if (e.getOp().referencedSize() == 1) {
            return e.getOp().printSimpleImpl(vars);
        }

        // We don't want to assign a variable to a variable
        // so there is this shortcut here.
        if (e.getOp() instanceof Var) {
            return ((Var) e.getOp()).getVar();
        }

        Optional<Triple<ExprOp, Character, String>> var = vars.stream().filter(t -> t.getFirst() == e.getOp()).findFirst();

        // If the expression already has a variable then just print the variable.
        if (var.isPresent()) {
            Triple<ExprOp, Character, String> v = var.get();
            return v.getSecond().toString();
        } else {
            char v;
            if (!vars.isEmpty()) {
                v = (char) (vars.get(vars.size() - 1).getSecond() + 1);
            } else {
                v = 'a';
            }
            vars.add(new Triple<>(e.getOp(), v, ""));

            int idx = vars.size() - 1;
            // Get the initializer for the variable.
            vars.get(idx).setThird(e.getOp().printSimpleImpl(vars));
            // Return just the variable name.
            return String.valueOf(v);
        }
    }

    /**
     * Yes, this PERFORMANCE CRITICAL code could be more efficient...
     */
    private String printSimpleImpl(ArrayList<Triple<ExprOp, Character, String>> vars) {
        Function4<String, Expr, Expr, ArrayList<Triple<ExprOp, Character, String>>, String> bin_op = (String op, Expr l, Expr r, ArrayList<Triple<ExprOp, Character, String>> _vars) -> {
            int pred = this.precedence();

            String left;
            if (pred > l.getOp().precedence() && l.getOp().referencedSize() == 1) {
                left = String.format("(%s)", ExprOp.printSimpleRc(l, _vars));
            } else {
                left = ExprOp.printSimpleRc(l, _vars);
            }

            String right;
            if (pred > r.getOp().precedence() && r.getOp().referencedSize() == 1) {
                right = String.format("(%s)", ExprOp.printSimpleRc(r, _vars));
            } else {
                right = ExprOp.printSimpleRc(r, _vars);
            }

            return String.format("%s %s %s", left, op, right);
        };

        Function3<String, Expr, ArrayList<Triple<ExprOp, Character, String>>, String> un_op = (String op, Expr i, ArrayList<Triple<ExprOp, Character, String>> _vars) -> {
            if (this.precedence() > i.getOp().precedence() && i.getOp().referencedSize() == 1) {
                return String.format("%s(%s)", op, ExprOp.printSimpleRc(i, _vars));
            } else {
                return String.format("%s%s", op, ExprOp.printSimpleRc(i, _vars));
            }
        };

        switch (this.type()) {
            case Const:
                return String.valueOf(((Const) this).getVal());
            case Var:
                return ((Var) this).getVar();
            case Add: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("+", l, r, vars);
            }
            case Sub: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("-", l, r, vars);
            }
            case Mul: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("*", l, r, vars);
            }
            case Div: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("/", l, r, vars);
            }
            case Neg: {
                SingleExprOp op = (SingleExprOp) this;
                Expr i = op.getExpr();
                return un_op.apply("-", i, vars);
            }
            case And: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("&", l, r, vars);
            }
            case Or: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("|", l, r, vars);
            }
            case Xor: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("^", l, r, vars);
            }
            case Not: {
                SingleExprOp op = (SingleExprOp) this;
                Expr i = op.getExpr();
                return un_op.apply("~", i, vars);
            }
            case Shl: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply("<<", l, r, vars);
            }
            case Shr: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply(">>", l, r, vars);
            }
            case Sar: {
                DoubleExprOp op = (DoubleExprOp) this;
                Expr l = op.getLeft();
                Expr r = op.getRight();
                return bin_op.apply(">>>", l, r, vars);
            }
            default:
                throw new RuntimeException("unknown operation type: " + this.type());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // Stores a mapping of (sub)expressions to variables.
        ArrayList<Triple<ExprOp, Character, String>> vars = new ArrayList<>();
        String l = this.printSimpleImpl(vars);
        Collections.reverse(vars);
        for (Triple<ExprOp, Character, String> triple : vars) {
            sb.append(String.format("%s = %s", triple.getSecond(), triple.getThird())).append('\n');
        }
        sb.append(l);
        return sb.toString();
    }

    public enum ExprOpType {
        Const,
        Var,
        Add,
        Sub,
        Mul,
        Div,
        Neg,
        And,
        Or,
        Xor,
        Not,
        Shl,
        Shr,
        Sar;
    }
}
