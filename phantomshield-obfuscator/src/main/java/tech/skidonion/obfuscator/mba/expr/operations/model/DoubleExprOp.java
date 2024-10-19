package tech.skidonion.obfuscator.mba.expr.operations.model;

import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;

public abstract class DoubleExprOp extends ExprOp {
    private final Expr left;
    private final Expr right;

    public DoubleExprOp(Expr left, Expr right) {
        this.left = left;
        this.right = right;
//        left.setParent(this);
//        right.setParent(this);
    }

    public Expr getRight() {
        return right;
    }

    public Expr getLeft() {
        return left;
    }
}
