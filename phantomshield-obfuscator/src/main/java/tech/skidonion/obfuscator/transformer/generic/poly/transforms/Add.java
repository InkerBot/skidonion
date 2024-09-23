package tech.skidonion.obfuscator.transformer.generic.poly.transforms;


import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public class Add extends Transformation {
    private final int value;

    public Add(int value) {
        this.value = value;
    }

    @Override
    public int transform(int i) {
        return i + value;
    }

    @Override
    public Transformation reversed() {
        return new Subtract(value);
    }

    @Override
    public OperationType type() {
        return OperationType.ADD;
    }

    public int getValue() {
        return value;
    }
}
