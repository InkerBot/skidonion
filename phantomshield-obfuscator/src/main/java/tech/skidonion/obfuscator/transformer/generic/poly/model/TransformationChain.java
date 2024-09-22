package tech.skidonion.obfuscator.transformer.generic.poly.model;

import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;

import java.util.ArrayList;
import java.util.function.Function;

public class TransformationChain extends ArrayList<Transformation> implements Function<Integer, Integer> {
    private static final long serialVersionUID = 6587027146192465729L;
    private TransformationChain reverse;

    @Override
    public Integer apply(Integer t) {
        int c = t;
        for (int i = 0; i < size(); i++)
            c = get(i).transform(c);
        return c;
    }

    public int[] transform(byte[] src) {
        int[] buffer = new int[src.length];
        int check;
        for (int pos = 0; pos < buffer.length; pos++) {
            int c = ((int) src[pos]) & 0xFF;
            buffer[pos] = this.apply(c);
            check = this.reverse().apply(buffer[pos]);
            if ((byte) (check & 0xFF) != src[pos]) {
                throw new RuntimeException("generated poly algorithm overflow?");
            }
        }
        return buffer;
    }

    public TransformationChain reverse() {
        if (reverse == null) {
            TransformationChain reverse = new TransformationChain();
            for (int i = size() - 1; i >= 0; --i)
                reverse.add(get(i).reversed());
            reverse.reverse = this;
            return this.reverse = reverse;
        }
        return reverse;
    }

}
