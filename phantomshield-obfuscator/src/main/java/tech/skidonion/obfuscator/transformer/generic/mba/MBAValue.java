package tech.skidonion.obfuscator.transformer.generic.mba;

public class MBAValue {

    private final int value;

    private final MBAValueType type;

    private MBAValue(int value, MBAValueType type) {
        this.value = value;
        this.type = type;
    }

    public static MBAValue value(int intValue) {
        return new MBAValue(intValue, MBAValueType.INT_VALUE);
    }

    /**
     * @param index you may use ASMUtils.computeMaxLocals(MethodNode)
     */
    public static MBAValue local(int index) {
        return new MBAValue(index, MBAValueType.LOCAL);
    }

    public MBAValueType getType() {
        return type;
    }

    /**
     * @return return index if type is LOCAL, or value.
     */
    public int getValue() {
        return value;
    }

    public enum MBAValueType {
        LOCAL, INT_VALUE;
    }
}
