package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification;

import java.util.concurrent.ThreadLocalRandom;

public class BufferContext {
    private int index;
    private byte[] originBuffer;
    private byte[] encryptedBuffer;

    public BufferContext(int index, byte[] originBuffer, byte[] encryptedBuffer) {
        this.index = index;
        this.originBuffer = originBuffer;
        this.encryptedBuffer = encryptedBuffer;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getOriginBuffer() {
        return originBuffer;
    }

    public byte[] getEncryptedBuffer() {
        return encryptedBuffer;
    }

    public BufferPredicate generatePredicate() {
        boolean condition = ThreadLocalRandom.current().nextBoolean();
        int index = ThreadLocalRandom.current().nextInt(originBuffer.length);
        byte origin = originBuffer[index];
        byte buffer;
        // ture = equals
        // false = non-equals
        if (condition) {
            buffer = origin;
        } else {
            do {
                buffer = (byte) ThreadLocalRandom.current().nextInt();
            } while (buffer == origin);
        }
        return new BufferPredicate(index, buffer, condition);
    }

    public String generateCondition(String realValue, String fakeValue) {
        return generateCondition(ConditionType.VALUE, realValue, fakeValue);
    }

    /**
     * @return condition
     */
    public String generateCondition(ConditionType type, String realValue, String fakeValue) {
        StringBuilder sb = new StringBuilder();
        boolean position = ThreadLocalRandom.current().nextBoolean();
        BufferContext.BufferPredicate predicate = this.generatePredicate();
        if (type == ConditionType.CODE_BLOCK) {
            sb.append("if (");
        } else {
            sb.append("((");
        }
        String left;
        String right;
        if (predicate.condition()) {
            if (position) {
                left = realValue;
                right = fakeValue;
                sb.append("!");
            } else {
                left = fakeValue;
                right = realValue;
            }
        } else {
            if (position) {
                left = realValue;
                right = fakeValue;
            } else {
                left = fakeValue;
                right = realValue;
                sb.append("!");
            }
        }

        sb.append("(inlines::__buffer[").append(this.getIndex()).append("][").append(predicate.getIndex()).append("] ^ static_cast<jbyte>(").append(predicate.getBuffer()).append("))");
        if (type == ConditionType.CODE_BLOCK) {
            sb.append(") { ").append(left).append(" } else { ").append(right).append(" }");
        } else {
            sb.append(") ? (").append(left).append(") : (").append(right).append("))");
        }

        return sb.toString();
    }

    public static class BufferPredicate {
        private final int index;
        private final byte buffer;
        private final boolean condition;

        public BufferPredicate(int index, byte buffer, boolean condition) {
            this.index = index;
            this.buffer = buffer;
            this.condition = condition;
        }

        public int getIndex() {
            return index;
        }

        public byte getBuffer() {
            return buffer;
        }

        public boolean condition() {
            return condition;
        }
    }

    public static enum ConditionType {
        CODE_BLOCK, VALUE;
    }
}
