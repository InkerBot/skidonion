package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.MethodHandler;

import java.lang.invoke.MethodHandles;

class PreprocessorUtilsTest {

    @Test
    void performanceClassLoader() {
        long last = System.currentTimeMillis();

        for (int i = 0; i < 100000; i++) {
            ClassLoader classLoader = Class.class.getClassLoader();
        }
        System.out.println(System.currentTimeMillis() - last + "ms");
    }

    @Test
    void performanceLookup() {
        long last = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            MethodHandles.lookup();
        }
        System.out.println(System.currentTimeMillis() - last + "ms");
    }
}