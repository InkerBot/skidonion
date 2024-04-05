package dummy;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

import static tech.skidonion.obfuscator.annotations.NativeObfuscation.VirtualMachine.TIGER_RED;

public class TestNative {
    public String a;
    @NativeObfuscation(virtualize = TIGER_RED)
    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            System.out.println(i);
        }
    }
}
