package tech.skidonion.obfuscator.mba.helper;


import tech.skidonion.obfuscator.utils.commons.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Valuation {
    private List<Pair<String, BigInteger>> vals;
    private MissingValue missing;

    /**
     * Constructor for an empty valuation that will panic when any variable is requested.
     */
    public Valuation() {
        this.vals = new ArrayList<>();
        this.missing = MissingValue.panic();
    }

    /**
     * A valuation that returns zero for any variable.
     */
    public static Valuation zero() {
        Valuation valuation = new Valuation();
        valuation.missing = MissingValue.zero();
        return valuation;
    }

    /**
     * A valuation that returns a random value for any variable.
     */
    public static Valuation random(int bits) {
        Valuation valuation = new Valuation();
        valuation.missing = MissingValue.random(bits);
        return valuation;
    }

    /**
     * Initializes a valuation from a list of pairs of variables and values.
     */
    public static Valuation fromVecPanic(List<Pair<String, BigInteger>> vals) {
        Valuation valuation = new Valuation();
        valuation.vals = vals;
        valuation.missing = MissingValue.panic();
        return valuation;
    }

    /**
     * Initializes a valuation from a list of pairs of variables and values.
     */
    public static Valuation fromVecZero(List<Pair<String, BigInteger>> vals) {
        Valuation valuation = new Valuation();
        valuation.vals = vals;
        valuation.missing = MissingValue.zero();
        return valuation;
    }

    /**
     * Initializes a valuation from a list of pairs of variables and values.
     */
    public static Valuation fromVecRandom(List<Pair<String, BigInteger>> vals, int bits) {
        Valuation valuation = new Valuation();
        valuation.vals = vals;
        valuation.missing = MissingValue.random(bits);
        return valuation;
    }

    /*Returns the value of a variable.*/
    public BigInteger value(String name) {
        for (Pair<String, BigInteger> pair : vals) {
            if (pair.getFirst().equals(name)) {
                return pair.getSecond();
            }
        }

        // If not found, use the missing valuation.
        BigInteger newVal = missing.handleMissing(name);
        vals.add(new Pair<>(name, newVal));
        return newVal;
    }

    /**
     * Sets the value of a variable.
     */
    public void setValue(String name, BigInteger value) {
        for (Pair<String, BigInteger> pair : vals) {
            if (pair.getFirst().equals(name)) {
                pair.setSecond(value);
                return;
            }
        }
        vals.add(new Pair<>(name, value));
    }

    /**
     * What should be done for a variable that is not found in the valuation.
     */
    public abstract static class MissingValue {
        private static final Random RANDOM = new Random();
        private static final MissingValue PANIC = new EnumPanic(MissingValueType.Panic);
        private static final MissingValue ZERO = new EnumZero(MissingValueType.Zero);

        public static MissingValue panic() {
            return PANIC;
        }

        public static MissingValue zero() {
            return ZERO;
        }

        public static MissingValue random(int bits) {
            return new EnumRandom(MissingValueType.Random, bits);
        }

        private final MissingValueType type;

        private MissingValue(MissingValueType type) {
            this.type = type;
        }

        public MissingValueType getType() {
            return type;
        }

        public abstract BigInteger handleMissing(String name);

        public enum MissingValueType {
            Panic, Zero, Random;
        }

        private static class EnumRandom extends MissingValue {
            private final int bits;

            private EnumRandom(MissingValueType type, int bits) {
                super(type);
                this.bits = bits;
            }

            @Override
            public BigInteger handleMissing(String name) {
                return new BigInteger(bits, RANDOM);
            }
        }

        private static class EnumPanic extends MissingValue {

            private EnumPanic(MissingValueType type) {
                super(type);
            }

            @Override
            public BigInteger handleMissing(String name) {
                throw new RuntimeException("Variable " + name + " not found in valuation.");
            }
        }

        private static class EnumZero extends MissingValue {
            private EnumZero(MissingValueType type) {
                super(type);
            }

            @Override
            public BigInteger handleMissing(String name) {
                return BigInteger.ZERO;
            }
        }
    }
}
