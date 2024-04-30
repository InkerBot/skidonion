package interfaces;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Main {
    @NativeObfuscation
    public static void main(String[] args) {
        new ClassA().shout();
    }
}
