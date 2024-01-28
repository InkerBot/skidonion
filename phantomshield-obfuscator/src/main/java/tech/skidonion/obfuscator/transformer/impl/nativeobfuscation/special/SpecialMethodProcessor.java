package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.special;

import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public interface SpecialMethodProcessor {
    String preProcess(MethodContext context);
    void postProcess(MethodContext context);
}
