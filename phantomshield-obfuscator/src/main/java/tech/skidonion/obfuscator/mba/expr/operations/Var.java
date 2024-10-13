package tech.skidonion.obfuscator.mba.expr.operations;

import tech.skidonion.obfuscator.mba.expr.ExprOp;

public class Var extends ExprOp {
    private final String var;

    public Var(String var) {
        this.var = var;
    }

    public String getVar() {
        return var;
    }

    @Override
    public ExprOpType type() {
        return ExprOpType.Var;
    }

}
