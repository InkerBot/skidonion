package tech.skidonion.obfuscator.asm.accesses;

import tech.skidonion.obfuscator.asm.MethodWrapper;

/**
 * Wrapper for MethodNode access flags.
 */
public class MethodAccess implements Access {
    private MethodWrapper wrapper;

    public MethodAccess(MethodWrapper wrapper) {
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
        return (ACC_SYNCHRONIZED & wrapper.getAccessFlags()) != 0;
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
        return badAccessCheck("VOLATILE");
    }

    @Override
    public boolean isBridge() {
        return (ACC_BRIDGE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isStaticPhase() {
        return badAccessCheck("STATIC_PHASE");
    }

    @Override
    public boolean isVarargs() {
        return (ACC_VARARGS & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isTransient() {
        return badAccessCheck("TRANSIENT");
    }

    @Override
    public boolean isNative() {
        return (ACC_NATIVE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isInterface() {
        return badAccessCheck("INTERFACE");
    }

    @Override
    public boolean isAbstract() {
        return (ACC_ABSTRACT & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isStrict() {
        return (ACC_STRICT & wrapper.getAccessFlags()) != 0;
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
                String.format("%s.%s%s is a method and cannot be checked for the access flag %s",
                        wrapper.getOwner().getOriginalName(), wrapper.getOriginalName(), wrapper.getOriginalDescription(),
                        type
                ));
    }
}
