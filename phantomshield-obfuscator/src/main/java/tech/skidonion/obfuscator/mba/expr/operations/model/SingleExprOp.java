package tech.skidonion.obfuscator.mba.expr.operations.model;

import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.ExprOp;

public abstract class SingleExprOp extends ExprOp {
    private final Expr expr;

    public SingleExprOp(Expr expr) {
        this.expr = expr;
//        expr.setParent(this);
    }

    public Expr getExpr() {
        return expr;
    }
}
