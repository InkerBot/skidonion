package pack.tests.basics.cross;

import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;

@LoadAfterLogin(value = "基础用户组", priority = 2)
public class Top extends Abst1 implements Inte {

    public void run() {
        if (add(1, 2) == 3) {
            if (mul(2, 3) == 6) {
                System.out.println("PASS");
                return;
            }
        }
        System.out.println("FAIL");
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
