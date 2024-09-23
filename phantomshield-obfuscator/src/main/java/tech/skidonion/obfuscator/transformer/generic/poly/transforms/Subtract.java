package tech.skidonion.obfuscator.transformer.generic.poly.transforms;


import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public class Subtract extends Transformation {
    private final int value;

    public Subtract(int value) {
        this.value = value;
    }

    @Override
    public int transform(int i) {
        return i - value;
    }

    @Override
    public Transformation reversed() {
        return new Add(value);
    }

    @Override
    public OperationType type() {
        return OperationType.SUBTRACT;
    }

    public int getValue() {
        return value;
    }
}
