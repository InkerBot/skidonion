package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.commons.SimpleRemapper;

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
}
