package tech.skidonion.obfuscator.transformer.generic.poly.transforms;

import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Rotation;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public class RotateLeft extends Rotation {
    public RotateLeft(int bits) {
        super(bits);
    }

    @Override
    public int transform(int i) {
        return (i >>> lhs()) | (i << rhs());
    }

    @Override
    public Transformation reversed() {
        return new RotateRight(getValue());
    }

    @Override
    public OperationType type() {
        return OperationType.ROTATE_LEFT;
    }
}
