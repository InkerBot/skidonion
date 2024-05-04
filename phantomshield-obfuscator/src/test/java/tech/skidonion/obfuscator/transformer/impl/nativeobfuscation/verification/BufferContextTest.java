package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.Buffer;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

class BufferContextTest {
    BufferContext buffer;

    @BeforeEach
    void setUp() {
        byte[] key = new byte[32];
        byte[] src = new byte[256];
        byte[] dst = new byte[256];
        byte[] s = new byte[256];
        ThreadLocalRandom.current().nextBytes(src);
        System.arraycopy(src, 0, dst, 0, src.length);
        generate_key:
        {
            int _i, _j = 0;
            byte[] k = new byte[256];
            byte tmp;
            for (_i = 0; _i < 256; _i++) {
                s[_i] = (byte) _i;
                k[_i] = key[_i % 32];
            }
            for (_i = 0; _i < 256; _i++) {
                _j = (_j + s[_i] + k[_i]) & 0xFF;
                tmp = s[_i];
                s[_i] = s[_j];
                s[_j] = tmp;
            }
        }

        encrypt_buffer:
        {
            int _i = 0, _j = 0, _t;
            byte tmp;
            for (int _k = 0; _k < 256; _k++) {
                _j = (_j + s[_i]) & 0xFF;
                tmp = s[_i];
                s[_i] = s[_j];
                s[_j] = tmp;
                _t = (s[_i] + s[_j]) & 0xFF;
                dst[_k] = (byte) (dst[_k] ^ (int) s[_t]);
                _i = (_i + 1) & 0xFF;
            }
        }

//        System.out.println(Arrays.toString(src));
        buffer = new BufferContext(0, src, dst);
    }

    @Test
    void predicate() {
        for (int i = 0; i < 10; i++) {
            System.out.println(buffer.generateCondition("123", "345"));
        }

        for (int i = 0; i < 10; i++) {
            System.out.println(buffer.generateCondition(BufferContext.ConditionType.CODE_BLOCK, "printf(\"true halo\\n\");", "printf(\"fake halo\\n\");"));
        }
    }
}