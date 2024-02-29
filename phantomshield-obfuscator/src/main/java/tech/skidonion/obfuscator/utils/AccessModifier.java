package tech.skidonion.obfuscator.utils;


public enum AccessModifier {
    UNCHANGED,
    PUBLIC,
    PROTECTED,
    PRIVATE;

    public String getFormattedName() {
        return "ACC:" + super.toString();
    }

    public AccessFlags transform(AccessFlags access) {
        if (this == AccessModifier.PUBLIC) {
            access.setPublic();
        } else if (this == AccessModifier.PROTECTED) {
            access.setProtected();
        } else if (this == AccessModifier.PRIVATE) {
            access.setPrivate();
        }
        return access;
    }

}
