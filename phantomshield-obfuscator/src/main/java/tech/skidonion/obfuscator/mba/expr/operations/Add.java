package tech.skidonion.obfuscator.mba.expr.operations;

import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.operations.model.DoubleExprOp;

public class Add extends DoubleExprOp {

    public Add(Expr left, Expr right) {
        super(left, right);
    }

    @Override
    public ExprOpType type() {
        return ExprOpType.Add;
    }
}
