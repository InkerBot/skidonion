package pack.tests.bench;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Calc {
    @NativeObfuscation.Inline
    public static int count = 0;

    @NativeObfuscation.Inline
    public static void runAll() {
//        System.out.println("[WARNING]: Huge performance drop, so we down to 1000 times loop");
        long start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            call(100);
            runAdd();
            runStr();
        }
        System.out.println("Calc: " + (System.currentTimeMillis() - start) + "ms");
        if (count != 30000)
            throw new RuntimeException("[ERROR]: Errors occurred in calc!");
    }

    @NativeObfuscation.Inline
    private static void call(int i) {
        if (i == 0)
            count++;
        else
            call(i - 1);
    }

    @NativeObfuscation.Inline
    private static void runAdd() {
        double i = 0d;
        while (i < 100.1d) {
            i += 0.99d;
        }
        count++;
    }

    @NativeObfuscation.Inline
    private static void runStr() {
        String str = "";
        while (str.length() < 101) {
            str += "ax";
        }
        count++;
    }
}
