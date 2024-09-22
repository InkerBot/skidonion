package tech.skidonion.obfuscator.transformer.generic.poly.transforms;


import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public class Xor extends Transformation {
    private final int value;

    public Xor(int value) {
        this.value = value;
    }

    @Override
    public int transform(int i) {
        return i ^ value;
    }

    @Override
    public Transformation reversed() {
        return this;
    }

    @Override
    public OperationType type() {
        return OperationType.XOR;
    }

    public int getValue() {
        return value;
    }
}
