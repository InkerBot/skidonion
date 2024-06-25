package pack;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class TestInline {

    private String prefix;

    TestInline(String prefix) {
        this.prefix = prefix;
    }

    public static void main(String[] args) {
        System.out.println(new TestInline("Prefix-").concat("content"));
    }

    @NativeObfuscation.Inline
    public String concat(String a) {
        return this.prefix + a;
    }
}
