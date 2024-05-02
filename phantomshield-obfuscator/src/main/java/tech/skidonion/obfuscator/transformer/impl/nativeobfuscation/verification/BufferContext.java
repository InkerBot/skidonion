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
                buffer = encryptedBuffer[index];
            } while (buffer == origin);
        }
        return new BufferPredicate(index, buffer, condition);
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

        public boolean isCondition() {
            return condition;
        }
    }
}
