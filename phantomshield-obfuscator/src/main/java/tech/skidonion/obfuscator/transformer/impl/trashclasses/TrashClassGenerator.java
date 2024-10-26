package tech.skidonion.obfuscator.transformer.impl.trashclasses;

import lombok.val;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.generator.BibleGenerator;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class TrashClassGenerator implements Opcodes {
    private final PhantomShield obfuscator;
    private final Dictionary dictionary;

    /**
     * value:
     * List: a list collects all same retrun type methods and fields
     * Pair:
     * first: member declare
     * second: parent
     */
    @SuppressWarnings("unchecked")
    private final List<Pair<TrashClass.MemberDeclare, TrashClass>>[] returnTypeMap = new List[Type.METHOD];
    private final List<TrashClass> interfaces = new ArrayList<>();
    private final List<TrashClass> abstractions = new ArrayList<>();
    private final List<TrashClass> plain = new ArrayList<>();
    private final List<TrashClass> classes = new ArrayList<>();

    public TrashClassGenerator(PhantomShield obfuscator) {
        this.obfuscator = obfuscator;
        this.dictionary = obfuscator.getDictionary().copy();
    }

    public void generate(String name, int methods, int fields) {
        val trash = TrashClass._plain(name);
        val virtualMethods = (int) (methods * ThreadLocalRandom.current().nextDouble());
        val staticMethods = methods - virtualMethods;
        val virtualFields = (int) (fields * ThreadLocalRandom.current().nextDouble());
        val staticFields = fields - virtualFields;

        for (int i = 0; i < virtualMethods; i++) {
            trash.addVirtualMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < staticMethods; i++) {
            trash.addStaticMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < virtualFields; i++) {
            trash.addVirtualField(dictionary.next(), generateRandomFieldDesc());
        }
        for (int i = 0; i < staticFields; i++) {
            TrashClass.MemberDeclare declare = trash.addStaticField(dictionary.next(), generateRandomFieldDesc());
            this.solveMember(declare.getReturnType(), declare, trash);
        }

        int interfaces = Math.min(ThreadLocalRandom.current().nextInt(4), this.interfaces.size());
        for (int i = 0; i < interfaces; i++) {
            trash.addInterface(this.interfaces.get(ThreadLocalRandom.current().nextInt(this.interfaces.size())));
        }

        if (!abstractions.isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
            trash.setSuperClass(abstractions.get(ThreadLocalRandom.current().nextInt(abstractions.size())));
        }
        this.plain.add(trash);
        this.classes.add(trash);
    }

    public void generateInterface(String name, int methods, int fields) {
        val trash = TrashClass._interface(name);
        val virtualMethods = (int) (methods * ThreadLocalRandom.current().nextDouble());
        val staticMethods = methods - virtualMethods;

        for (int i = 0; i < virtualMethods; i++) {
            trash.addAbstractMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < staticMethods; i++) {
            trash.addStaticMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < fields; i++) {
            TrashClass.MemberDeclare declare = trash.addStaticField(dictionary.next(), generateRandomFieldDesc());
            this.solveMember(declare.getReturnType(), declare, trash);
        }

        int interfaces = Math.min(ThreadLocalRandom.current().nextInt(4), this.interfaces.size());
        for (int i = 0; i < interfaces; i++) {
            trash.addInterface(this.interfaces.get(ThreadLocalRandom.current().nextInt(this.interfaces.size())));
        }
        this.interfaces.add(trash);
        this.classes.add(trash);
    }

    public void generateAbstraction(String name, int methods, int fields) {
        val trash = TrashClass._abstract(name);
        val virtualMethods = (int) (methods * ThreadLocalRandom.current().nextDouble());
        val staticMethods = methods - virtualMethods;
        val virtualFields = (int) (fields * ThreadLocalRandom.current().nextDouble());
        val staticFields = fields - virtualFields;

        for (int i = 0; i < virtualMethods; i++) {
            trash.addAbstractMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < staticMethods; i++) {
            trash.addStaticMethod(dictionary.next(), generateRandomMethodDesc());
        }
        for (int i = 0; i < virtualFields; i++) {
            trash.addVirtualField(dictionary.next(), generateRandomFieldDesc());
        }
        for (int i = 0; i < staticFields; i++) {
            TrashClass.MemberDeclare declare = trash.addStaticField(dictionary.next(), generateRandomFieldDesc());
            this.solveMember(declare.getReturnType(), declare, trash);
        }

        int interfaces = Math.min(ThreadLocalRandom.current().nextInt(4), this.interfaces.size());
        for (int i = 0; i < interfaces; i++) {
            trash.addInterface(this.interfaces.get(ThreadLocalRandom.current().nextInt(this.interfaces.size())));
        }

        if (!abstractions.isEmpty() && ThreadLocalRandom.current().nextBoolean()) {
            trash.setSuperClass(abstractions.get(ThreadLocalRandom.current().nextInt(abstractions.size())));
        }
        this.abstractions.add(trash);
        this.classes.add(trash);
    }

    public Map<String, byte[]> build() {
        Map<String, byte[]> classes = new HashMap<>();

        for (TrashClass clz : interfaces) {
            val node = new ClassNode();
            node.name = clz.getName();
            node.version = V1_8;
            node.superName = "java/lang/Object";
            node.access = ACC_PUBLIC | ACC_ABSTRACT | ACC_INTERFACE;

            for (TrashClass inte : clz.getInterfaces()) {
                node.interfaces.add(inte.getName());
            }

            for (TrashClass.MemberDeclare declare : clz.getAbstractions()) {
                node.methods.add(generateAbstractMethod(declare));
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticFields()) {
                node.fields.add(generateStaticField(declare, true));
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticMethods()) {
                node.methods.add(generateMethod(declare, true));
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            classes.put(node.name, writer.toByteArray());
        }

        for (TrashClass clz : abstractions) {
            val node = new ClassNode();
            node.name = clz.getName();
            node.version = V1_8;
            node.superName = clz.getSuperClass() == null ? "java/lang/Object" : clz.getSuperClass().getName();
            node.access = ACC_PUBLIC | ACC_SUPER | ACC_ABSTRACT;

            val init = new MethodNode();
            init.name = "<init>";
            init.desc = "()V";
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            init.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", "()V"));
            init.instructions.add(new InsnNode(RETURN));
            node.methods.add(init);

            for (TrashClass inte : clz.getInterfaces()) {
                node.interfaces.add(inte.getName());
            }

            for (TrashClass.MemberDeclare declare : clz.getAbstractions()) {
                node.methods.add(generateAbstractMethod(declare));
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticFields()) {
                node.fields.add(generateStaticField(declare, false));
            }

            for (TrashClass.MemberDeclare declare : clz.getVirtualFields()) {
                node.fields.add(generateField(declare));
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticMethods()) {
                node.methods.add(generateMethod(declare, true));
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            classes.put(node.name, writer.toByteArray());
        }

        for (TrashClass clz : plain) {
            val node = new ClassNode();
            node.name = clz.getName();
            node.version = V1_8;
            node.superName = clz.getSuperClass() == null ? "java/lang/Object" : clz.getSuperClass().getName();
            node.access = ACC_PUBLIC | ACC_SUPER;

            val init = new MethodNode();
            init.name = "<init>";
            init.desc = "()V";
            init.instructions.add(new VarInsnNode(ALOAD, 0));
            init.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", "()V"));
            init.instructions.add(new InsnNode(RETURN));
            node.methods.add(init);

            for (TrashClass inte : clz.getInterfaces()) {
                node.interfaces.add(inte.getName());
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticFields()) {
                node.fields.add(generateStaticField(declare, false));
            }

            for (TrashClass.MemberDeclare declare : clz.getVirtualFields()) {
                node.fields.add(generateField(declare));
            }

            for (TrashClass.MemberDeclare declare : clz.getStaticMethods()) {
                node.methods.add(generateMethod(declare, true));
            }
            HashSet<TrashClass.MemberDeclare> declares = new HashSet<>(clz.getVirtualMethods());
            findAllAbstractMethods(clz, declares);
            for (TrashClass.MemberDeclare declare : declares) {
                node.methods.add(generateMethod(declare, false));
            }

            ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            classes.put(node.name, writer.toByteArray());
        }

        return classes;
    }

    private static MethodNode generateAbstractMethod(TrashClass.MemberDeclare declare) {
        val node = new MethodNode();
        node.name = declare.getName();
        node.desc = declare.getDesc();
        node.access = ACC_PUBLIC | ACC_ABSTRACT;
        return node;
    }

    private MethodNode generateMethod(TrashClass.MemberDeclare declare, boolean isStatic) {
        val node = new MethodNode();
        node.name = declare.getName();
        node.desc = declare.getDesc();
        node.access = ACC_PUBLIC | (isStatic ? ACC_STATIC : 0);

        int opAmount = ThreadLocalRandom.current().nextInt(5);
        for (int i = 0; i < opAmount; i++) {
            node.instructions.add(new LabelNode());
            node.instructions.add(generateTrashCodeOperation());
        }
        node.instructions.add(new LabelNode());
        if (declare.getReturnType().getSort() != Type.VOID) {
            node.instructions.add(generatePushOperation(declare.getReturnType()));
        }
        node.instructions.add(new InsnNode(ASMUtils.getReturnOpcode(declare.getReturnType())));

        return node;
    }

    private InsnList generateTrashCodeOperation() {
        if (!this.classes.isEmpty()) {
            TrashClass clz = this.classes.get(ThreadLocalRandom.current().nextInt(this.classes.size()));
            switch (clz.getType()) {
                case PLAIN:
                    switch (ThreadLocalRandom.current().nextInt(3)) {
                        case 0:
                            return generateMethodOperation(clz.getRandomVirtualMethod(), clz.getName(), false);
                        case 1:
                            return generateMethodOperation(clz.getRandomStaticMethod(), clz.getName(), true);
                        case 2:
                            return generateFieldOperation(clz.getRandomStaticField(), clz.getName());
                    }
                    break;
                case ABSTRACT:
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        return generateMethodOperation(clz.getRandomStaticMethod(), clz.getName(), true);
                    } else {
                        return generateFieldOperation(clz.getRandomStaticField(), clz.getName());
                    }
                case INTERFACE:
                    return generateMethodOperation(clz.getRandomStaticMethod(), clz.getName(), true);
            }
        }
        return new InsnList();
    }

    private InsnList generateMethodOperation(TrashClass.MemberDeclare declare, String owner, boolean isStatic) {
        val insn = new InsnList();
        if (declare == null) {
            return insn;
        }
        if (!isStatic) {
            insn.add(new TypeInsnNode(NEW, owner));
            insn.add(new InsnNode(DUP));
            insn.add(new MethodInsnNode(INVOKESPECIAL, owner, "<init>", "()V"));
        }
        for (Type argument : declare.getArgumentTypes()) {
            insn.add(generatePushOperation(argument));
        }
        insn.add(new MethodInsnNode(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, owner, declare.getName(), declare.getDesc()));
        InsnNode pop = generatePop(declare.getReturnType());
        if (pop != null) {
            insn.add(pop);
        }
        return insn;
    }

    private InsnList generateFieldOperation(TrashClass.MemberDeclare declare, String owner) {
        val insn = new InsnList();
        if (declare == null) {
            return insn;
        }
        insn.add(generatePushOperation(declare.getReturnType()));
        insn.add(new FieldInsnNode(PUTSTATIC, owner, declare.getName(), declare.getDesc()));
        return insn;
    }

    private AbstractInsnNode generatePushOperation(Type type) {
        val sort = type.getSort();
        val list = returnTypeMap[sort];
        if (ThreadLocalRandom.current().nextBoolean() && list != null && list.isEmpty()) {
            val pair = list.get(ThreadLocalRandom.current().nextInt(list.size()));
            val declare = pair.getFirst();
            val parent = pair.getSecond();
            return (new FieldInsnNode(GETSTATIC, parent.getName(), declare.getName(), declare.getDesc()));
        } else {
            switch (sort) {
                case Type.BOOLEAN:
                    return (new InsnNode(ThreadLocalRandom.current().nextBoolean() ? ICONST_0 : ICONST_1));
                case Type.BYTE:
                    return (new IntInsnNode(BIPUSH, (byte) ThreadLocalRandom.current().nextInt()));
                case Type.CHAR:
                case Type.SHORT:
                    return (new IntInsnNode(SIPUSH, (short) ThreadLocalRandom.current().nextInt()));
                case Type.INT:
                    return (ASMUtils.getNumberInsn(ThreadLocalRandom.current().nextInt()));
                case Type.LONG:
                    return (new LdcInsnNode(ThreadLocalRandom.current().nextLong()));
                case Type.FLOAT:
                    return (new LdcInsnNode(ThreadLocalRandom.current().nextFloat()));
                case Type.DOUBLE:
                    return (new LdcInsnNode(ThreadLocalRandom.current().nextDouble()));
                case Type.OBJECT:
                    return (new LdcInsnNode(BibleGenerator.generate()));
                default:
                    throw new RuntimeException("Unsupported type: " + type);
            }
        }
    }

    private static FieldNode generateStaticField(TrashClass.MemberDeclare declare, boolean isFinal) {
        return new FieldNode(ACC_PUBLIC | ACC_STATIC | (isFinal ? ACC_FINAL : 0), declare.getName(), declare.getDesc(), "", randomValue(declare.getReturnType()));
    }

    private static FieldNode generateField(TrashClass.MemberDeclare declare) {
        return new FieldNode(ACC_PUBLIC, declare.getName(), declare.getDesc(), "", randomValue(declare.getReturnType()));
    }


    private static void findAllAbstractMethods(TrashClass clz, Set<TrashClass.MemberDeclare> declares) {
        declares.addAll(clz.getAbstractions());
        if (clz.getSuperClass() != null) {
            findAllAbstractMethods(clz.getSuperClass(), declares);
        }
        for (TrashClass inte : clz.getInterfaces()) {
            findAllAbstractMethods(inte, declares);
        }
    }

    private void solveMember(Type type, TrashClass.MemberDeclare declare, TrashClass clz) {
        int sort = type.getSort();
        if (this.returnTypeMap[sort] == null) {
            this.returnTypeMap[sort] = new ArrayList<>();
        }
        this.returnTypeMap[sort].add(new Pair<>(declare, clz));
    }

    private static InsnNode generatePop(Type type) {
        int size = type.getSize();
        if (size == 0) {
            return null;
        } else {
            return new InsnNode(POP + size - 1);
        }
    }

    private static Object randomValue(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return ThreadLocalRandom.current().nextBoolean();
            case Type.BYTE:
                return (int) (byte) ThreadLocalRandom.current().nextInt();
            case Type.CHAR:
            case Type.SHORT:
                return (int) (short) ThreadLocalRandom.current().nextInt();
            case Type.INT:
                return ThreadLocalRandom.current().nextInt();
            case Type.LONG:
                return ThreadLocalRandom.current().nextLong();
            case Type.FLOAT:
                return ThreadLocalRandom.current().nextFloat();
            case Type.DOUBLE:
                return ThreadLocalRandom.current().nextDouble();
            case Type.OBJECT:
                return BibleGenerator.generate();
            default:
                return null;
        }
    }


    private static final String[] TYPE_USED = {
            "Ljava/lang/String;",
            "I", // int
            "J", // long
            "B", // byte
            "C", // char
            "S", // short
            "Z", // boolean
            "F", // float
            "D", // double
            "V", // void
    };


    public static String generateRandomMethodDesc() {
        int amount = ThreadLocalRandom.current().nextInt(6);
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < amount; i++) {
            sb.append(TYPE_USED[ThreadLocalRandom.current().nextInt(TYPE_USED.length - 1)]);
        }
        sb.append(')').append(TYPE_USED[ThreadLocalRandom.current().nextInt(TYPE_USED.length)]);
        return sb.toString();
    }

    public static String generateRandomFieldDesc() {
        return TYPE_USED[ThreadLocalRandom.current().nextInt(TYPE_USED.length - 1)];
    }
}
