package pack.tests.basics.inner;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Exec {
    public int fuss = 1;

    @NativeObfuscation.Inline
    public void addF() {
        fuss += 2;
    }

    public class Inner {
        int i;

        public Inner(int p) {
            i = p;
        }

        @NativeObfuscation.Inline
        public void doAdd() {
            addF();
            fuss += i;
        }
    }
}
