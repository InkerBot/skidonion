package tech.skidonion.obfuscator.mba.expr;

import tech.skidonion.obfuscator.utils.commons.Pair;
import tech.skidonion.obfuscator.utils.commons.PeekableCharacterIterator;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.expr.operations.*;
import tech.skidonion.obfuscator.mba.expr.operations.model.DoubleExprOp;
import tech.skidonion.obfuscator.mba.expr.operations.model.SingleExprOp;
import tech.skidonion.obfuscator.mba.helper.BigIntHelper;
import tech.skidonion.obfuscator.mba.helper.Valuation;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class Expr {
    private ExprOp op;
//    private ExprOp parent;

    public Expr(ExprOp op) {
        this.op = op;
    }

    public ExprOp getOp() {
        return op;
    }

    public void setOp(ExprOp op) {
        this.op = op;
    }

//    public ExprOp getParent() {
//        return parent;
//    }
//
//    public void setParent(ExprOp parent) {
//        this.parent = parent;
//    }

    /**
     * Evaluate an expression.
     */
    public BigInteger eval(Valuation v, int bits) {
        ArrayList<Pair<ExprOp, BigInteger>> cache = new ArrayList<>();
        return evalImpl(this, v, bits, cache);
    }

    private static BigInteger evalImpl(Expr e, Valuation v, int bits, ArrayList<Pair<ExprOp, BigInteger>> cache) {
//        if (e.parent != null) {
//            // This is a common subexpression.
//            // We don't want to evaluate it twice.
//            // So we look it up in the cache.
//            for (Pair<ExprOp, BigInteger> pair : cache) {
//                if (pair.getKey() == e.parent) {
//                    return new BigInteger(pair.getValue().toByteArray());
//                }
//            }
//        }
        BigInteger _v;
        switch (e.op.type()) {
            case Const:
                _v = new BigInteger(((Const) e.op).getVal().toByteArray());
                break;
            case Var:
                _v = v.value(((Var) e.op).getVar());
                break;
            case Add: {
                Add m = (Add) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).add(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Sub: {
                Sub m = (Sub) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).subtract(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Mul: {
                Mul m = (Mul) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).multiply(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Div: {
                Div m = (Div) e.op;
                BigInteger r = evalImpl(m.getRight(), v, bits, cache);
                // We don't want division by zero to panic,
                // so we define it as zero.
                if (r.equals(BigInteger.ZERO)) {
                    _v = BigInteger.ZERO;
                } else {
                    _v = evalImpl(m.getLeft(), v, bits, cache);
                }
                break;
            }
            case Neg: {
                Neg m = (Neg) e.op;
                _v = evalImpl(m.getExpr(), v, bits, cache).negate();
                break;
            }
            case And: {
                And m = (And) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).and(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Or: {
                Or m = (Or) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).or(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Xor: {
                Xor m = (Xor) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).xor(evalImpl(m.getRight(), v, bits, cache));
                break;
            }
            case Not: {
                Not m = (Not) e.op;
                _v = evalImpl(m.getExpr(), v, bits, cache).not();
                break;
            }
            case Shl: {
                Shl m = (Shl) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).shiftLeft(evalImpl(m.getRight(), v, bits, cache).remainder(BigInteger.valueOf(bits)).intValue());
                break;
            }
            case Shr: {
                Shr m = (Shr) e.op;
                _v = evalImpl(m.getLeft(), v, bits, cache).shiftRight(evalImpl(m.getRight(), v, bits, cache).remainder(BigInteger.valueOf(bits)).intValue());
                break;
            }
            case Sar: {
                Sar m = (Sar) e.op;
                _v = BigIntHelper.keepSignedBits(evalImpl(m.getLeft(), v, bits, cache), bits).shiftRight(evalImpl(m.getRight(), v, bits, cache).remainder(BigInteger.valueOf(bits)).intValue());
                break;
            }
            default:
                throw new RuntimeException("unknown operation type: " + e.op.type());
        }

        _v = BigIntHelper.keepBits(_v, bits);
//        if (e.parent != null) {
//            // This is a common subexpression.
//            // We want to cache it.
//            cache.add(new Pair<>(e.getOp(), new BigInteger(_v.toByteArray())));
//        }
        return _v;
    }

    /**
     * Substitutes an expression for a variable.
     * If the Rc's in this expression are shared with
     * other expressions then this will also substitute in those.
     */
    public void substitute(String var, Expr s) {
        ArrayList<ExprOp> visited = new ArrayList<>();
        substituteImpl(this, var, s, visited);
    }

    private static void substituteImpl(Expr e, String var, Expr s, ArrayList<ExprOp> visited) {
        boolean recurse;
        if (visited.contains(e.getOp())) {
            recurse = false;
        } else {
            visited.add(e.getOp());
            recurse = true;
        }
        // SAFETY: This is okay because we make sure with extra logic
        // that this is never encountered twice.
        switch (e.op.type()) {
            case Const:
                break;
            case Var: {
                if (((Var) e.op).getVar().equals(var)) {
                    e.op = s.clone().getOp();
                }
                break;
            }
            case Add:
            case Sub:
            case Mul:
            case Div:
            case And:
            case Or:
            case Xor:
            case Shl:
            case Shr:
            case Sar: {
                DoubleExprOp m = ((DoubleExprOp) e.op);
                if (recurse) {
                    substituteImpl(m.getLeft(), var, s, visited);
                    substituteImpl(m.getRight(), var, s, visited);
                }
                break;
            }
            case Neg:
            case Not: {
                if (recurse) {
                    substituteImpl(((SingleExprOp) e.op).getExpr(), var, s, visited);
                }
            }
        }
    }

    /**
     * Parse an expression from a string.
     */
    public static Optional<Expr> fromString(String s) {
        s = s.replaceAll("\\s+", "");
        return parse(new PeekableCharacterIterator(s), 0);
    }


    // pre 0: parse as much as possible
    // ...
    // pre 15: parse as little as possible
    private static Optional<Expr> parse(PeekableCharacterIterator it, int pre) {
        Character c = it.peek();
        if (c == null)
            return Optional.empty();

        Expr e;
        if (c == '(') {
            it.next();
            Optional<Expr> _e = parse(it, 0);
            if (!_e.isPresent()) return Optional.empty();
            if (it.hasNext() && it.next() == ')') {
                e = _e.get();
            } else {
                return Optional.empty();
            }
        } else if (c == '~') {
            it.next();
            Optional<Expr> _e = parse(it, 15);
            if (!_e.isPresent()) return Optional.empty();
            e = new Expr(new Not(_e.get()));
        } else if (c == '-') {
            it.next();
            Optional<Expr> _e = parse(it, 15);
            if (!_e.isPresent()) return Optional.empty();
            e = new Expr(new Neg(_e.get()));
        } else if (Character.isAlphabetic(c)) {
            it.next();
            StringBuilder var = new StringBuilder(c.toString());
            for (; ; ) {
                if ((c = it.peek()) == null) break;

                if (!Character.isAlphabetic(c) && c != '_') break;

                var.append(c);
                it.next();
            }

            e = new Expr(new Var(var.toString()));
        } else if (Character.isDigit(c)) {
            StringBuilder sb = new StringBuilder();
            do {
                sb.append(it.next());
            } while (it.hasNext() && Character.isDigit(it.peek()));
            e = new Expr(new Const(new BigInteger(sb.toString())));
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
                case '<':
                case '>':
                    op_pre = 4;
                    break;
                case '+':
                case '-':
                    op_pre = 5;
                    break;
                case '*':
                case '/':
                    op_pre = 6;
                    break;
                case ')':
                    return Optional.of(e);
                default:
                    return Optional.empty();
            }
            if (op_pre <= pre) {
                return Optional.of(e);
            }

            // If the current operators precedence is higher than
            // the one whose subexpression we are currently parsing
            // then we need to finish this operator first.
            it.next();
            switch (c) {
                case '+': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Add(e, _e.get()));
                    break;
                }
                case '-': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Sub(e, _e.get()));
                    break;
                }
                case '*': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Mul(e, _e.get()));
                    break;
                }
                case '/': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Div(e, _e.get()));
                    break;
                }
                case '&': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new And(e, _e.get()));
                    break;
                }
                case '|': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Or(e, _e.get()));
                    break;
                }
                case '^': {
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Xor(e, _e.get()));
                    break;
                }
                case '<': {
                    if (!it.hasNext() || it.next() != '<') return Optional.empty();
                    Optional<Expr> _e = parse(it, op_pre);
                    if (!_e.isPresent()) return Optional.empty();
                    e = new Expr(new Shl(e, _e.get()));
                    break;
                }
                case '>': {
                    if (!it.hasNext() || it.next() != '>') return Optional.empty();

                    if (Objects.equals(it.peek(), '>')) {
                        it.next();
                        Optional<Expr> _e = parse(it, op_pre);
                        if (!_e.isPresent()) return Optional.empty();
                        e = new Expr(new Sar(e, _e.get()));
                    } else {
                        Optional<Expr> _e = parse(it, op_pre);
                        if (!_e.isPresent()) return Optional.empty();
                        e = new Expr(new Shr(e, _e.get()));
                    }
                    break;
                }
                default:
                    throw new RuntimeException("Unreachable code");
            }
        }
    }

    @Override
    public String toString() {
        return op.toString();
    }

    @Override
    public Expr clone() {
        return new Expr(op.clone());
    }
}
