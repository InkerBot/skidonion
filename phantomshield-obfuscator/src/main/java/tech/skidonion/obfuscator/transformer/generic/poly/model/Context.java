package tech.skidonion.obfuscator.transformer.generic.poly.model;

public class Context {
    private final int[] encoded;
    private final TransformationChain forward, reverse;

    public Context(int[] encoded, TransformationChain forward, TransformationChain reverse) {
        this.encoded = encoded;
        this.forward = forward;
        this.reverse = reverse;
    }

    public Context(TransformationChain forward, TransformationChain reverse) {
        this(null, forward, reverse);
    }


    public boolean hasEncoded() {
        return encoded != null;
    }

    public int[] getEncoded() {
        return encoded;
    }

    public TransformationChain getForward() {
        return forward;
    }

    public TransformationChain getReverse() {
        return reverse;
    }
}
