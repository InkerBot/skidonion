package interfaces;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

@NativeObfuscation
public class ClassA implements MyInterface {
    @NativeObfuscation
    public void shout() {
        System.out.println(test);
    }
}
