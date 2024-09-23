package tech.skidonion.obfuscator.transformer.generic.poly.model;

import tech.skidonion.obfuscator.transformer.generic.poly.transforms.*;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.model.Transformation;
import tech.skidonion.obfuscator.utils.commons.RandomWrapper;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class PolymorphicEngine implements Engine {
    private static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private Random userRandom;

    public PolymorphicEngine() {
    }

    public PolymorphicEngine(Random rand) {
        this.userRandom = rand;
    }

    @Override
    public Context transform(byte[] src) {
        TransformationChain forward = generateForward();
        return new Context(forward.transform(src), forward, forward.reverse());
    }

    @Override
    public Context generateChain() {
        TransformationChain forward = generateForward();
        return new Context(forward, forward.reverse());
    }

    private TransformationChain generateForward() {
        TransformationChain forward = new TransformationChain();
        for (int i = 0; i < 16; i++)
            forward.add(generateTransformation());
        return forward;
    }

    private Transformation generateTransformation() {
        switch (nextInt(6)) {
            case 0:
                return new Add(getRandom().nextInt());
            case 1:
                return new Not();
            case 2:
                return new RotateLeft(nextInt(31) + 1);
            case 3:
                return new RotateRight(nextInt(31) + 1);
            case 4:
                return new Subtract(getRandom().nextInt());
            case 5:
                return new Xor(getRandom().nextInt());
            default:
                throw new RuntimeException("Unreachable code");
        }
    }


    /* Random generator */

    public void setUserRandom(Random userRandom) {
        this.userRandom = userRandom;
    }

    private Random getRandom() {
        return userRandom == null ? RANDOM : userRandom;
    }

    public int nextInt(int bound) {
        return RandomWrapper.nextInt(getRandom(), bound);
    }


}
