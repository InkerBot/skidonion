package tech.skidonion.obfuscator.transformer.generic.poly.transforms;


import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public class Not extends Transformation {
    public Not() {
    }

    @Override
    public int transform(int i) {
        return ~i;
    }

    @Override
    public Transformation reversed() {
        return this;
    }

    @Override
    public OperationType type() {
        return OperationType.NOT;
    }
}
