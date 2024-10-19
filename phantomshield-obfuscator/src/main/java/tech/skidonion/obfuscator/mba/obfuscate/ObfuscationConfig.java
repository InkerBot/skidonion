package tech.skidonion.obfuscator.mba.obfuscate;

import tech.skidonion.obfuscator.mba.obfuscate.rewrite.RewriteTries;
import tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl.FiniteFail;

public class ObfuscationConfig {
    private final int rewriteVars;
    private final int rewriteExprDepth;
    private final int rewriteExprCount;
    private final RewriteTries rewriteTries;

    public ObfuscationConfig(int rewriteVars, int rewriteExprDepth, int rewriteExprCount, RewriteTries rewriteTries) {
        this.rewriteVars = rewriteVars;
        this.rewriteExprDepth = rewriteExprDepth;
        this.rewriteExprCount = rewriteExprCount;
        this.rewriteTries = rewriteTries;
    }


    public static ObfuscationConfig defaultConfig() {
        return new ObfuscationConfig(4, 3, 24, new FiniteFail(128));
    }

    public int getRewriteVars() {
        return rewriteVars;
    }

    public int getRewriteExprDepth() {
        return rewriteExprDepth;
    }

    public int getRewriteExprCount() {
        return rewriteExprCount;
    }

    public RewriteTries getRewriteTries() {
        return rewriteTries;
    }
}