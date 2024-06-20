package pack.tests.dirty.clinit;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Called implements CalledFace {
    private static String ret = "FAIL";

    static {
        ret = "PASS";
    }

    @NativeObfuscation.Inline
    private static void doCallInlined() {
        ClinitCall.callMeInlined();
    }

    public void doPrint(Class<?> clazz) {
        int cnHash = clazz.getName().hashCode();
        if (Called.class.getName().hashCode() != cnHash) {
            ret = "FAIL";
        }
        if (ClinitCall.class.getName().hashCode() == ClinitCall.getIpp()
        ) {
            // Dumb : just let the javac don't optimize it
            ret = "FAIL";
        }
        if (ClinitCall.called) {
            ret = "FAIL";
        }
        ClinitCall.callMeInlined();
        if (!ClinitCall.called) {
            ret = "FAIL";
        }

        System.out.println(ret);
    }
}
