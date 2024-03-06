package dummy;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

import static tech.skidonion.obfuscator.annotations.NativeObfuscation.VirtualMachine.*;

public class TestNative {
    @NativeObfuscation(virtualize = TIGER_RED)
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }
    }
}
