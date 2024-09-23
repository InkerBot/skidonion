package tech.skidonion.obfuscator.transformer.generic.poly.visitors;


import tech.skidonion.obfuscator.transformer.generic.poly.model.Context;
import tech.skidonion.obfuscator.transformer.generic.poly.model.TransformationChain;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.*;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

public interface Visitor<T> {
    T initialise(Context ctx);

    void finalise(T in);

    default T visit(Context ctx) {
        T t = initialise(ctx);
        visit(ctx.getReverse(), t);
        finalise(t);
        return t;
    }

    default void visit(TransformationChain chain, T in) {
        for (Transformation element : chain)
            visit(element, in);
    }

    /* Visit methods */

    void visit(Add a, T in);

    void visit(Not n, T in);

    void visit(RotateLeft rl, T in);

    void visit(RotateRight rr, T in);

    void visit(Subtract s, T in);

    void visit(Xor x, T in);

    /* Double dispatch visitor methods */

    default void visit(Transformation t, T in) {
        switch (t.type()) {
            case ADD:
                visit((Add) t, in);
                break;
            case NOT:
                visit((Not) t, in);
                break;
            case ROTATE_LEFT:
                visit((RotateLeft) t, in);
                break;
            case ROTATE_RIGHT:
                visit((RotateRight) t, in);
                break;
            case SUBTRACT:
                visit((Subtract) t, in);
                break;
            case XOR:
                visit((Xor) t, in);
                break;
            default:
                throw new IllegalStateException("Unimplemented transformation double dispatch");
        }
    }
}
