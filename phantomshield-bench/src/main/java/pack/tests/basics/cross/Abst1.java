package pack.tests.basics.cross;

import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;

@LoadAfterLogin(value = "基础用户组", priority = 1)
public abstract class Abst1 {
    public abstract int add(int a, int b);

    public int mul(int a, int b) {
        return a * b;
    }
}
