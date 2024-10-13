package tech.skidonion.obfuscator.mba.obfuscate.rewrite.impl;

import tech.skidonion.obfuscator.mba.obfuscate.rewrite.RewriteTries;

public class Infinite extends RewriteTries {
    @Override
    public int triesTimes() {
        return -1;
    }

    @Override
    public RewriteTriesType type() {
        return RewriteTriesType.Infinite;
    }
}
