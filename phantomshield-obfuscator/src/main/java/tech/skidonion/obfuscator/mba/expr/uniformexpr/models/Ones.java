package tech.skidonion.obfuscator.mba.expr.uniformexpr.models;

import tech.skidonion.obfuscator.mba.expr.uniformexpr.UExpr;

public class Ones extends UExpr {
    public static final Ones ONES = new Ones();

    @Override
    public UExprType type() {
        return UExprType.Ones;
    }
}
