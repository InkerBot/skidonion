package tech.skidonion.obfuscator.asm.remapper;

import java.util.Map;

/**
 * Custom implementation of ASM's SimpleRemapper taking in account for field descriptions.
 */
public class MemberRemapper extends SimpleRemapper {
    public MemberRemapper(final Map<String, String> mappings) {
        super(mappings);
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        String remappedName = map(owner + '.' + name + '.' + desc);
        return (remappedName != null) ? remappedName : name;
    }

    @Override
    public String mapAnnotationAttributeName(final String descriptor, final String desc, final String name) {
        String remappedName = map(descriptor.substring(1, descriptor.length() - 1) + '.' + name + "()" + desc);
        return (remappedName != null) ? remappedName : name;
    }

}
