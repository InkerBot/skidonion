package tech.skidonion.obfuscator.filter;

import java.util.*;

import static tech.skidonion.obfuscator.utils.StringUtils.convertClassNameToPath;
import static tech.skidonion.obfuscator.utils.StringUtils.toDescriptor;

public class FilterInformation {
    private final List<String> owner_annotations = new ArrayList<>();
    private String owner;
    private final List<String> owner_implements = new ArrayList<>();
    private String owner_extends;
    private final List<String> member_annotations = new ArrayList<>();
    private String member;
    private String descriptor;
    private FilterType type;

    private FilterInformation() {
    }

    public static FilterInformation resolve(String expression) {
        FilterInformation filter = new FilterInformation();
        String[] parts = Objects.requireNonNull(expression).split(" ");
        filter.type = FilterType.CLASS;

        boolean isMember = false;
        boolean hasImplements = false;
        boolean hasExtends = false;
        String returnType = null;

        for (int index = 0; index < parts.length; index++) {
            String part = parts[index];
            boolean isLast = index == parts.length - 1;

            if (hasExtends) {
                if (filter.owner_extends != null)
                    throw new RuntimeException("one class can't have two fathers: " + expression);
                filter.owner_extends = convertClassNameToPath(part);
                hasExtends = false;
            } else if (hasImplements) {
                filter.owner_implements.add(convertClassNameToPath(part));
                hasImplements = false;
            } else if (part.startsWith("@")) {
                part = toDescriptor(part.substring(1));
                if (isMember) filter.member_annotations.add(part);
                else filter.owner_annotations.add(part);
            } else if ("extends".equals(part)) {
                hasExtends = true;
            } else if ("implements".equals(part)) {
                hasImplements = true;
            } else {
                if (filter.owner == null) {
                    filter.owner = convertClassNameToPath(part);
                    isMember = true;
                } else if (returnType == null) {
                    returnType = toDescriptor(part);
                } else if (isLast) {
                    int methodIndex = part.indexOf("(");
                    if (methodIndex != -1) {
                        StringBuilder sb = new StringBuilder();
                        String[] arguments = part.substring(methodIndex + 1, part.length() - 1).split(",");
                        filter.type = FilterType.METHOD;
                        filter.member = part.substring(0, methodIndex);
                        sb.append("(");
                        for (String argument : arguments) {
                            sb.append(toDescriptor(argument));
                        }
                        sb.append(")");
                        sb.append(returnType);
                        filter.descriptor = sb.toString();
                    } else {
                        filter.type = FilterType.FIELD;
                        filter.member = part;
                        filter.descriptor = returnType;
                    }
                }
            }
        }
        return filter;
    }


    public List<String> getOwnerAnnotations() {
        return owner_annotations;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public List<String> getOwnerImplements() {
        return owner_implements;
    }

    public String getOwnerExtends() {
        return owner_extends;
    }

    public void setOwnerExtends(String owner_extends) {
        this.owner_extends = owner_extends;
    }

    public List<String> getMemberAnnotations() {
        return member_annotations;
    }

    public String getMember() {
        return member;
    }

    public void setMember(String member) {
        this.member = member;
    }

    public String getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(String descriptor) {
        this.descriptor = descriptor;
    }

    public FilterType getType() {
        return type;
    }

    public void setType(FilterType type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        FilterInformation filterInformation = (FilterInformation) object;
        return Objects.equals(owner_annotations, filterInformation.owner_annotations) && Objects.equals(owner, filterInformation.owner) && Objects.equals(owner_implements, filterInformation.owner_implements) && Objects.equals(owner_extends, filterInformation.owner_extends) && Objects.equals(member_annotations, filterInformation.member_annotations) && Objects.equals(member, filterInformation.member) && Objects.equals(descriptor, filterInformation.descriptor) && type == filterInformation.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner_annotations, owner, owner_implements, owner_extends, member_annotations, member, descriptor, type);
    }

    @Override
    public String toString() {
        return "Pattern{" +
                "owner_annotations=" + Arrays.toString(owner_annotations.toArray()) +
                ", owner='" + owner + '\'' +
                ", owner_implements=" + Arrays.toString(owner_implements.toArray()) +
                ", owner_extends='" + owner_extends + '\'' +
                ", member_annotations=" + Arrays.toString(member_annotations.toArray()) +
                ", member='" + member + '\'' +
                ", descriptor='" + descriptor + '\'' +
                ", type=" + type +
                '}';
    }

    public enum FilterType {
        CLASS,
        METHOD,
        FIELD
    }
}
