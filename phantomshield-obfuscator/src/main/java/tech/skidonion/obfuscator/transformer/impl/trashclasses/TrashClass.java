package tech.skidonion.obfuscator.transformer.impl.trashclasses;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.objectweb.asm.Type;

import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class TrashClass {
    private final TrashClassType type;
    private final String name;
    private TrashClass superClass;
    private final Set<TrashClass> interfaces = new HashSet<>();
    private final Set<MemberDeclare> virtualMethods = new HashSet<>();
    private final Set<MemberDeclare> staticMethods = new HashSet<>();
    private final Set<MemberDeclare> virtualFields = new HashSet<>();
    private final Set<MemberDeclare> staticFields = new HashSet<>();
    private final Set<MemberDeclare> abstractions = new HashSet<>();

    private List<MemberDeclare> virtualMethodsC;
    private List<MemberDeclare> staticMethodsC;
    private List<MemberDeclare> virtualFieldsC;
    private List<MemberDeclare> staticFieldsC;

    private TrashClass(TrashClassType type, String name, TrashClass superClass) {
        this.type = type;
        this.name = name;
        this.superClass = superClass;
    }

    public MemberDeclare addAbstractMethod(String name, String desc) {
        if (type == TrashClassType.PLAIN)
            throw new UnsupportedOperationException("Cannot add abstract methods of type PLAIN");
        MemberDeclare member;
        this.abstractions.add(member = new MemberDeclare(name, desc));
        return member;
    }

    public MemberDeclare addStaticMethod(String name, String desc) {
        MemberDeclare member;
        this.staticMethods.add(member = new MemberDeclare(name, desc));
        return member;
    }

    public MemberDeclare addVirtualMethod(String name, String desc) {
        MemberDeclare member;
        this.virtualMethods.add(member = new MemberDeclare(name, desc));
        return member;
    }

    public MemberDeclare addStaticField(String name, String desc) {
        MemberDeclare member;
        this.staticFields.add(member = new MemberDeclare(name, desc));
        return member;
    }

    public MemberDeclare addVirtualField(String name, String desc) {
        MemberDeclare member;
        this.virtualFields.add(member = new MemberDeclare(name, desc));
        return member;
    }

    public void addInterface(TrashClass trashClass) {
        if (trashClass.type != TrashClassType.INTERFACE)
            throw new UnsupportedOperationException("Cannot add interfaces of type NOT INTERFACE");
        this.interfaces.add(trashClass);
    }

    public void setSuperClass(TrashClass trashClass) {
        if (trashClass.type == TrashClassType.INTERFACE || type == TrashClassType.INTERFACE) {
            throw new UnsupportedOperationException("Illegal");
        }
        this.superClass = trashClass;
    }

    public MemberDeclare getRandomStaticField() {
        if (this.staticFieldsC == null) {
            this.staticFieldsC = new ArrayList<>(this.staticFields);
        }
        if (this.staticFieldsC.isEmpty()) {
            return null;
        }
        return this.staticFieldsC.get(ThreadLocalRandom.current().nextInt(this.staticFieldsC.size()));
    }

    public MemberDeclare getRandomStaticMethod() {
        if (this.staticMethodsC == null) {
            this.staticMethodsC = new ArrayList<>(this.staticMethods);
        }
        if (this.staticMethodsC.isEmpty()) {
            return null;
        }
        return this.staticMethodsC.get(ThreadLocalRandom.current().nextInt(this.staticMethodsC.size()));
    }


    public MemberDeclare getRandomVirtualField() {
        if (this.virtualFieldsC == null) {
            this.virtualFieldsC = new ArrayList<>(this.virtualFields);
        }
        if (this.virtualFieldsC.isEmpty()) {
            return null;
        }
        return this.virtualFieldsC.get(ThreadLocalRandom.current().nextInt(this.virtualFieldsC.size()));
    }

    public MemberDeclare getRandomVirtualMethod() {
        if (this.virtualMethodsC == null) {
            this.virtualMethodsC = new ArrayList<>(this.virtualMethods);
        }
        if (this.virtualMethodsC.isEmpty()) {
            return null;
        }
        return this.virtualMethodsC.get(ThreadLocalRandom.current().nextInt(this.virtualMethodsC.size()));
    }


    public static TrashClass _plain(String name) {
        return new TrashClass(TrashClassType.PLAIN, name, null);
    }

    public static TrashClass _plain(String name, TrashClass superName) {
        return new TrashClass(TrashClassType.PLAIN, name, superName);
    }

    public static TrashClass _abstract(String name) {
        return new TrashClass(TrashClassType.ABSTRACT, name, null);
    }

    public static TrashClass _abstract(String name, TrashClass superName) {
        return new TrashClass(TrashClassType.ABSTRACT, name, superName);
    }

    public static TrashClass _interface(String name) {
        return new TrashClass(TrashClassType.INTERFACE, name, null);
    }

    public static TrashClass _interface(String name, TrashClass superName) {
        return new TrashClass(TrashClassType.INTERFACE, name, superName);
    }

    public enum TrashClassType {
        PLAIN, ABSTRACT, INTERFACE;
    }

    @Getter
    @EqualsAndHashCode
    public static class MemberDeclare {
        private final String name;
        private final String desc;
        @EqualsAndHashCode.Exclude
        private final boolean hasArguments;
        @EqualsAndHashCode.Exclude
        private final Type[] argumentTypes;
        @EqualsAndHashCode.Exclude
        private final Type returnType;

        public MemberDeclare(String name, String desc) {
            this.name = name;
            this.desc = desc;
            this.hasArguments = desc.startsWith("(");
            if (hasArguments) {
                this.argumentTypes = Type.getArgumentTypes(desc);
                this.returnType = Type.getReturnType(desc);
            } else {
                this.argumentTypes = null;
                this.returnType = Type.getType(desc);
            }
        }
    }
}
