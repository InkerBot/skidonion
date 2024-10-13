package tech.skidonion.obfuscator.mba.expr.operations;

import tech.skidonion.obfuscator.mba.expr.Expr;
import tech.skidonion.obfuscator.mba.expr.operations.model.SingleExprOp;

public class Neg extends SingleExprOp {

    public Neg(Expr expr) {
        super(expr);
    }

    @Override
    public ExprOpType type() {
        return ExprOpType.Neg;
    }
}
