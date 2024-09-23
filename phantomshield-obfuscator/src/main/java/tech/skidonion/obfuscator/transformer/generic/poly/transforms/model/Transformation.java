package tech.skidonion.obfuscator.transformer.generic.poly.transforms.model;

import java.util.Optional;

public abstract class Transformation {

    public enum OperationType {
        ADD, NOT, ROTATE_LEFT, ROTATE_RIGHT, SUBTRACT, XOR;
    }

    public Transformation() {
    }

    public abstract int transform(int i);

    public abstract Transformation reversed();

    public abstract OperationType type();

}
