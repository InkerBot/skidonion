package tech.skidonion.obfuscator.asm.accesses;

import org.objectweb.asm.Opcodes;

public class AccessFlags {
    private int flags;

    public AccessFlags(int flags) {
        this.flags = flags;
    }

    public boolean isPrivate() {
        return (this.flags & Opcodes.ACC_PRIVATE) != 0;
    }

    public boolean isProtected() {
        return (this.flags & Opcodes.ACC_PROTECTED) != 0;
    }

    public boolean isPublic() {
        return (this.flags & Opcodes.ACC_PUBLIC) != 0;
    }

    public boolean isSynthetic() {
        return (this.flags & Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isStatic() {
        return (flags & Opcodes.ACC_STATIC) != 0;
    }

    public boolean isEnum() {
        return (flags & Opcodes.ACC_ENUM) != 0;
    }

    public boolean isBridge() {
        return (flags & Opcodes.ACC_BRIDGE) != 0;
    }

    public boolean isFinal() {
        return (flags & Opcodes.ACC_FINAL) != 0;
    }

    public boolean isInterface() {
        return (flags & Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isAbstract() {
        return (flags & Opcodes.ACC_ABSTRACT) != 0;
    }

    public boolean isAnnotation() {
        return (flags & Opcodes.ACC_ANNOTATION) != 0;
    }

    public AccessFlags setPrivate() {
        this.setVisibility(Opcodes.ACC_PRIVATE);
        return this;
    }

    public AccessFlags setProtected() {
        this.setVisibility(Opcodes.ACC_PROTECTED);
        return this;
    }

    public AccessFlags setPublic() {
        this.setVisibility(Opcodes.ACC_PUBLIC);
        return this;
    }

    public AccessFlags setBridge() {
        flags |= Opcodes.ACC_BRIDGE;
        return this;
    }

    @Deprecated
    public AccessFlags setBridged() {
        return setBridge();
    }

    public void setVisibility(int visibility) {
        this.resetVisibility();
        this.flags |= visibility;
    }

    private void resetVisibility() {
        this.flags &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
    }

    public int getFlags() {
        return this.flags;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AccessFlags && ((AccessFlags) obj).flags == flags;
    }

    @Override
    public int hashCode() {
        return flags;
    }

}
