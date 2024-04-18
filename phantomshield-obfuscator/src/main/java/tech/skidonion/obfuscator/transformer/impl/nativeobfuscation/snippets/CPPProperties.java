package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets;

import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.impl.FastProperties;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.impl.NullSafetyProperties;

public class CPPProperties {

    public static String build(boolean nullSafety) {
        if (nullSafety) {
            return NullSafetyProperties.build();
        } else {
            return FastProperties.build();
        }
    }


}
