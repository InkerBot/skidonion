package tech.skidonion.obfuscator.mba.obfuscate.rewrite;

import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.FiniteFail;
import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.FiniteOriginal;
import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.Infinite;

public abstract class RewriteTries {

    public static RewriteTries infinite() {
        return new Infinite();
    }

    public static RewriteTries finitefail(int times) {
        return new FiniteFail(times);
    }

    public static RewriteTries finiteOriginal(int times) {
        return new FiniteOriginal(times);
    }

    public abstract int triesTimes();

    public abstract RewriteTriesType type();

    public enum RewriteTriesType {
        Infinite, FiniteFail, FiniteOriginal;
    }
}


