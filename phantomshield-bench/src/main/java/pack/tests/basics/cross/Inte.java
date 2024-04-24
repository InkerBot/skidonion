package pack.tests.basics.cross;

import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;

@LoadAfterLogin("基础用户组")
public interface Inte {
    public int mul(int a, int b);
}
