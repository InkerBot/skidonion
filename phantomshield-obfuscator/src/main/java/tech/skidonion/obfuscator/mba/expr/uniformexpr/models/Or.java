package tech.skidonion.obfuscator.mba.expr.uniformexpr.models;

import tech.skidonion.obfuscator.mba.expr.uniformexpr.UExpr;

public class Or extends UExpr {
    private final UExpr left;
    private final UExpr right;

    public Or(UExpr left, UExpr right) {
        this.left = left;
        this.right = right;
    }

    public UExpr getLeft() {
        return left;
    }

    public UExpr getRight() {
        return right;
    }

    @Override
    public UExprType type() {
        return UExprType.Or;
    }
}
