package tech.skidonion.obfuscator.trace.v2;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ChaCha20 {
    private static final byte[] magic_constant = "expand 32-byte k".getBytes(StandardCharsets.US_ASCII);

    // size 16
    private final int[] keystream32 = new int[16];
    private final byte[] keystream8 = new byte[64];
    private int position;
    private long counter;
    private final byte[] nonce;
    // size 16
    private final int[] state = new int[16];

    public ChaCha20(byte[] key /*32 bytes*/, byte[] nonce /*12 bytes*/, long counter) {
        this.state[0] = pack4(magic_constant, 0 * 4);
        this.state[1] = pack4(magic_constant, 1 * 4);
        this.state[2] = pack4(magic_constant, 2 * 4);
        this.state[3] = pack4(magic_constant, 3 * 4);
        this.state[4] = pack4(key, 0 * 4);
        this.state[5] = pack4(key, 1 * 4);
        this.state[6] = pack4(key, 2 * 4);
        this.state[7] = pack4(key, 3 * 4);
        this.state[8] = pack4(key, 4 * 4);
        this.state[9] = pack4(key, 5 * 4);
        this.state[10] = pack4(key, 6 * 4);
        this.state[11] = pack4(key, 7 * 4);

        // 64 bit counter initialized to zero by default.

        this.state[12] = (int) (counter & 0x0000_0000_FFFF_FFFFL);
        this.state[13] = pack4(nonce, 0 * 4) + (int) (counter >>> 32);
        this.state[14] = pack4(nonce, 1 * 4);
        this.state[15] = pack4(nonce, 2 * 4);

        this.counter = counter;
        this.nonce = nonce;
        this.position = 64;
    }

    private static int rotl32(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private static int pack4(byte[] a, int offset) {
        int res = 0;
        res |= (a[offset + 0] & 0xff) << 0 * 8;
        res |= (a[offset + 1] & 0xff) << 1 * 8;
        res |= (a[offset + 2] & 0xff) << 2 * 8;
        res |= (a[offset + 3] & 0xff) << 3 * 8;
        return res;
    }

    private static void unpack4(int src, byte[] dst, int offset) {
        dst[offset + 0] = (byte) ((src >>> 0 * 8) & 0xff);
        dst[offset + 1] = (byte) ((src >>> 1 * 8) & 0xff);
        dst[offset + 2] = (byte) ((src >>> 2 * 8) & 0xff);
        dst[offset + 3] = (byte) ((src >>> 3 * 8) & 0xff);
    }

    private static void QUARTERROUND(int[] x, int a, int b, int c, int d) {
        x[a] += x[b];
        x[d] = rotl32(x[d] ^ x[a], 16);
        x[c] += x[d];
        x[b] = rotl32(x[b] ^ x[c], 12);
        x[a] += x[b];
        x[d] = rotl32(x[d] ^ x[a], 8);
        x[c] += x[d];
        x[b] = rotl32(x[b] ^ x[c], 7);
    }

    private void nextBlock() {
        // This is where the crazy voodoo magic happens.
        // Mix the bytes a lot and hope that nobody finds out how to undo it.

        System.arraycopy(this.state, 0, this.keystream32, 0, 16);
        for (int i = 0; i < 10; i++) {
            QUARTERROUND(this.keystream32, 0, 4, 8, 12);
            QUARTERROUND(this.keystream32, 1, 5, 9, 13);
            QUARTERROUND(this.keystream32, 2, 6, 10, 14);
            QUARTERROUND(this.keystream32, 3, 7, 11, 15);
            QUARTERROUND(this.keystream32, 0, 5, 10, 15);
            QUARTERROUND(this.keystream32, 1, 6, 11, 12);
            QUARTERROUND(this.keystream32, 2, 7, 8, 13);
            QUARTERROUND(this.keystream32, 3, 4, 9, 14);
        }

        for (int i = 0; i < 16; i++) this.keystream32[i] += this.state[i];
        for (int i = 0; i < 16; i++) unpack4(this.keystream32[i], this.keystream8, i * 4);
//        int lower = state[12];
//        int higher = state[13];
        ++counter;
        ++state[12];
        if (0 == state[12]) {
            ++state[13];
        }
    }

    public synchronized byte[] xor(byte[] src) {
        byte[] dst = new byte[src.length];
        for (int i = 0; i < src.length; i++) {
            if (this.position >= 64) {
                this.nextBlock();
                this.position = 0;
            }
            dst[i] = (byte) (src[i] ^ this.keystream8[this.position]);
            this.position++;
        }
        return dst;
    }

    public long getCounter() {
        return counter;
    }

    public void setCounter(long counter) {
        this.counter = counter;
        this.state[12] = (int) (counter & 0x0000_0000_FFFF_FFFFL);
        this.state[13] = pack4(nonce, 0 * 4) + (int) (counter >>> 32);
    }
}
