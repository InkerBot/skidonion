package pack.tests.basics.cross;

import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;

@LoadAfterLogin(value = "基础用户组", priority = 0)
public interface Inte {
    public int mul(int a, int b);
}
