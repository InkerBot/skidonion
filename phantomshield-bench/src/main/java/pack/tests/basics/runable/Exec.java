package pack.tests.basics.runable;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Exec {
    @NativeObfuscation.Inline

    public static int i = 1;
    @NativeObfuscation.Inline
    private int d;

    public Exec(int delta) {
        d = delta;
    }

    @NativeObfuscation.Inline
    void doAdd() {
        try {
            Thread.sleep(200L);
        } catch (Exception ignored) {

        }
        i += d;
    }
}
