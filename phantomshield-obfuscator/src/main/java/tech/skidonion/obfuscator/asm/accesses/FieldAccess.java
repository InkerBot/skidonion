package tech.skidonion.obfuscator.asm.accesses;


import tech.skidonion.obfuscator.asm.FieldWrapper;

/**
 * Wrapper for FieldNode access flags.
 */
public class FieldAccess implements Access {
    private FieldWrapper wrapper;

    public FieldAccess(FieldWrapper wrapper) {
        this.wrapper = wrapper;
    }

    @Override
    public boolean isPublic() {
        return (ACC_PUBLIC & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isPrivate() {
        return (ACC_PRIVATE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isProtected() {
        return (ACC_PROTECTED & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isStatic() {
        return (ACC_STATIC & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isFinal() {
        return (ACC_FINAL & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isSuper() {
        return badAccessCheck("SYNCHRONIZED");
    }

    @Override
    public boolean isSynchronized() {
        return badAccessCheck("SYNCHRONIZED");
    }

    @Override
    public boolean isOpen() {
        return badAccessCheck("OPEN");
    }

    @Override
    public boolean isTransitive() {
        return badAccessCheck("TRANSITIVE");
    }

    @Override
    public boolean isVolatile() {
        return (ACC_VOLATILE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isBridge() {
        return badAccessCheck("BRIDGE");
    }

    @Override
    public boolean isStaticPhase() {
        return badAccessCheck("STATIC_PHASE");
    }

    @Override
    public boolean isVarargs() {
        return badAccessCheck("VARARGS");
    }

    @Override
    public boolean isTransient() {
        return (ACC_TRANSIENT & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isNative() {
        return badAccessCheck("NATIVE");
    }

    @Override
    public boolean isInterface() {
        return badAccessCheck("INTERFACE");
    }

    @Override
    public boolean isAbstract() {
        return badAccessCheck("ABSTRACT");
    }

    @Override
    public boolean isStrict() {
        return badAccessCheck("STRICT");
    }

    @Override
    public boolean isSynthetic() {
        return (ACC_SYNTHETIC & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isAnnotation() {
        return badAccessCheck("ANNOTATION");
    }

    @Override
    public boolean isEnum() {
        return badAccessCheck("ENUM");
    }

    @Override
    public boolean isMandated() {
        return badAccessCheck("MANDATED");
    }

    @Override
    public boolean isModule() {
        return badAccessCheck("MODULE");
    }

    @Override
    public boolean isDeprecated() {
        return (ACC_DEPRECATED & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean badAccessCheck(String type) {
        throw new RuntimeException(
                String.format("%s.%s with type %s is a field and cannot be checked for the access flag %s",
                        wrapper.getOwner().getOriginalName(), wrapper.getOriginalName(), wrapper.getOriginalDescription(),
                        type
                ));
    }
}
