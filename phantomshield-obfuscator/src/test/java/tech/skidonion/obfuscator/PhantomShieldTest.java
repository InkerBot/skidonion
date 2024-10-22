package tech.skidonion.obfuscator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PhantomShieldTest {

    @Test
    void testDirectoies() {
        StringBuilder builder = new StringBuilder("tech/skidonion/obfuscator/123");
        for (int index = builder.length(); (index = builder.lastIndexOf("/", index - 1)) != -1; ) {
            System.out.println(builder.substring(0, index + 1));
        }
    }
}