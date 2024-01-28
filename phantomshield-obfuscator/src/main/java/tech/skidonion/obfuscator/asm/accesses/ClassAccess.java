package tech.skidonion.obfuscator.asm.accesses;

import tech.skidonion.obfuscator.asm.ClassWrapper;

public class ClassAccess implements Access {
    private ClassWrapper wrapper;

    public ClassAccess(ClassWrapper wrapper) {
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
        return badAccessCheck("STATIC");
    }

    @Override
    public boolean isFinal() {
        return (ACC_FINAL & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isSuper() {
        return (ACC_SUPER & wrapper.getAccessFlags()) != 0;
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
        return badAccessCheck("VOLATILE");
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
        return badAccessCheck("TRANSIENT");
    }

    @Override
    public boolean isNative() {
        return badAccessCheck("NATIVE");
    }

    @Override
    public boolean isInterface() {
        return (ACC_INTERFACE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isAbstract() {
        return (ACC_ABSTRACT & wrapper.getAccessFlags()) != 0;
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
        return (ACC_ANNOTATION & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isEnum() {
        return (ACC_ENUM & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isMandated() {
        return badAccessCheck("MANDATED");
    }

    @Override
    public boolean isModule() {
        return (ACC_MODULE & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean isDeprecated() {
        return (ACC_DEPRECATED & wrapper.getAccessFlags()) != 0;
    }

    @Override
    public boolean badAccessCheck(String type) {
        throw new RuntimeException(
                String.format("%s is a class and cannot be checked for the access flag %s",
                        wrapper.getOriginalName(), type
                ));
    }
}
