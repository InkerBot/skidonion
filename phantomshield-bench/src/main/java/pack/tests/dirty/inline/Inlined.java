package pack.tests.dirty.inline;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Inlined {
    @NativeObfuscation.Inline
    private static int makeErr = 0;

    @NativeObfuscation.Inline
    public static void perform() {
        for (int i = 0; i < 10; i++) {
            makeErr++;
            addI(i);
        }
        System.out.println("makeErr: " + makeErr);
    }

    @NativeObfuscation.Inline
    private static void addI(int i) {
        makeErr++;
    }
}
