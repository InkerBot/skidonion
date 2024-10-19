package tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl;

import tech.skidonion.obfuscator.mba.obfuscate.rewrite.RewriteTries;

public class FiniteOriginal extends RewriteTries {
    private final int amount;

    public FiniteOriginal(final int amount) {
        this.amount = amount;
    }

    @Override
    public int triesTimes() {
        return this.amount;
    }

    @Override
    public RewriteTriesType type() {
        return null;
    }
}
