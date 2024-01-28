package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.ClassWriter;
import tech.skidonion.obfuscator.PhantomShield;

/**
 * Custom-implemented version of {@link ClassWriter} which doesn't use the internal classpath.
 */
public class CustomClassWriter extends ClassWriter {
    private PhantomShield obfuscator;

    public CustomClassWriter(int flags, PhantomShield obfuscator) {
        super(flags);
        this.obfuscator = obfuscator;
    }

    @Override
    protected String getCommonSuperClass(final String type1, final String type2) {
        if ("java/lang/Object".equals(type1) || "java/lang/Object".equals(type2))
            return "java/lang/Object";

        String first = deriveCommonSuperName(type1, type2);
        String second = deriveCommonSuperName(type2, type1);
        if (!"java/lang/Object".equals(first))
            return first;

        if (!"java/lang/Object".equals(second))
            return second;

        return getCommonSuperClass(obfuscator.getClassWrapper(type1).getSuperName(), obfuscator.getClassWrapper(type2).getSuperName());
    }

    private String deriveCommonSuperName(final String type1, final String type2) {
        ClassWrapper first = obfuscator.getClassWrapper(type1);
        ClassWrapper second = obfuscator.getClassWrapper(type2);
        if (obfuscator.isAssignableFrom(type1, type2))
            return type1;
        else if (obfuscator.isAssignableFrom(type2, type1))
            return type2;
        else if (first.getAccess().isInterface() || second.getAccess().isInterface())
            return "java/lang/Object";
        else {
            String temp;

            do {
                temp = first.getSuperName();
                first = obfuscator.getClassWrapper(temp);
            } while (!obfuscator.isAssignableFrom(temp, type2));
            return temp;
        }
    }
}
