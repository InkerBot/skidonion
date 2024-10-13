package tech.skidonion.obfuscator.mba.expr.uniformexpr.models;

import tech.skidonion.obfuscator.mba.expr.uniformexpr.UExpr;

public class Not extends UExpr {
    private final UExpr expr;

    public Not(UExpr expr) {
        this.expr = expr;
    }

    public UExpr getExpr() {
        return expr;
    }

    @Override
    public UExprType type() {
        return UExprType.Not;
    }
}
