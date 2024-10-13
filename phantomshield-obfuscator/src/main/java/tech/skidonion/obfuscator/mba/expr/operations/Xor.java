package tech.skidonion.obfuscator.mba.expr.operations;

import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.operations.model.DoubleExprOp;

public class Xor extends DoubleExprOp {

    public Xor(Expr left, Expr right) {
        super(left, right);
    }

    @Override
    public ExprOpType type() {
        return ExprOpType.Xor;
    }

}
