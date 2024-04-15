package pack.tests.basics.inner;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

public class Test {
    @NativeObfuscation.Inline
    public void run() {
        Exec exec = new Exec();
        Exec.Inner inner1 = exec.new Inner(3);
        inner1.doAdd();
        Exec.Inner inner2 = exec.new Inner(100);
        inner2.doAdd();
        if (exec.fuss == 108)
            System.out.println("PASS");
        else
            System.out.println("ERROR");
    }
}
