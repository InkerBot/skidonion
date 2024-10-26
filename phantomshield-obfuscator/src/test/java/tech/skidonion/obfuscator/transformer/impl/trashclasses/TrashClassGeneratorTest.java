package tech.skidonion.obfuscator.transformer.impl.trashclasses;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrashClassGeneratorTest {
    @Test
    void name() {
        new String();
        System.out.println(TrashClassGenerator.generateRandomMethodDesc());
        a("123", "456");
    }

    void a(String... a) {

    }
}