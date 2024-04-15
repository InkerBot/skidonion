package pack.tests.basics.ctrl;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Ctrl {
    @NativeObfuscation.Inline
    private String ret = "FAIL";

    @NativeObfuscation.Inline
    public void runt() {
        if ("a".equals("b")) {
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @NativeObfuscation.Inline
    public void runf() {
        try {
            runt();
        } catch (RuntimeException e) {
            ret = "PASS";
        }
        try {
            runt();
            ret = "FAIL";
        } catch (Exception e) {

        }
    }

    @NativeObfuscation.Inline
    public void run() {
        runf();
        System.out.println(ret);
    }
}
