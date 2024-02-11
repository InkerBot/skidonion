package tech.skidonion.obfuscator.asm.remapper;


import java.util.Map;

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
    public String mapLambdaMeta(final String owner, final String name, final String descriptor) {
        String remappedName = map(owner + '.' + name + descriptor);
        return remappedName == null ? name : remappedName;
    }

}
