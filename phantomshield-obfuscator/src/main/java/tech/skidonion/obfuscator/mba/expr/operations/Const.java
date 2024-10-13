package tech.skidonion.obfuscator.mba.expr.operations;

import tech.skidonion.obfuscator.mba.expr.ExprOp;

import java.math.BigInteger;

public class Const extends ExprOp {
    private final BigInteger val;

    public Const(final BigInteger val) {
        this.val = val;
    }

    public BigInteger getVal() {
        return val;
    }

    @Override
    public ExprOpType type() {
        return ExprOpType.Const;
    }

}
