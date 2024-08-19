package tech.skidonion.obfuscator.transformer.impl.renamer;

import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.utils.ASMUtils;

import java.util.HashSet;
import java.util.Set;

public class MixinSupport {
    private static final Set<String> MIXIN_ANNOTATIONS_FIELD = new HashSet<String>() {{
        add("Lorg/spongepowered/asm/mixin/Shadow;");
        add("Lorg/spongepowered/asm/mixin/Final;");
    }};

    private static final Set<String> MIXIN_ANNOTATIONS_METHOD = new HashSet<String>() {
        {
            add("Lorg/spongepowered/asm/mixin/Shadow;");
            add("Lorg/spongepowered/asm/mixin/Final;");
            add("Lorg/spongepowered/asm/mixin/Overwrite;");
            add("Lorg/spongepowered/asm/mixin/Accessor;");
            add("Lorg/spongepowered/asm/mixin/Invoker;");
        }
    };

    public static boolean isMixinMethod(MethodWrapper wrapper) {
        if (wrapper.getMethodNode().visibleAnnotations != null) {
            return wrapper.getMethodNode().visibleAnnotations.stream().anyMatch(annotation -> MIXIN_ANNOTATIONS_METHOD.contains(annotation.desc));
        }
        return false;
    }

    public static boolean isMixinField(FieldWrapper wrapper) {
        if (wrapper.getFieldNode().visibleAnnotations != null) {
            return wrapper.getFieldNode().visibleAnnotations.stream().anyMatch(annotation -> MIXIN_ANNOTATIONS_FIELD.contains(annotation.desc));
        }
        return false;
    }

}
