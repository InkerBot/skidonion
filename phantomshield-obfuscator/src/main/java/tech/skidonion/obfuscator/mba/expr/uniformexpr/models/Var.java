package tech.skidonion.obfuscator.mba.expr.uniformexpr.models;

import tech.skidonion.obfuscator.mba.expr.uniformexpr.UExpr;

public class Var extends UExpr {
    private String symbol;

    public Var(String symbol) {
        this.symbol = symbol;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    @Override
    public UExprType type() {
        return UExprType.Var;
    }
}
