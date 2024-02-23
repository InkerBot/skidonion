package tech.skidonion.obfuscator.utils;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Bytecode utilities for bytecode instructions.
 */
public class ASMUtils {
    public static boolean isInstruction(AbstractInsnNode insn) {
        return !(insn instanceof FrameNode) && !(insn instanceof LineNumberNode) && !(insn instanceof LabelNode);
    }

    public static boolean isReturn(int opcode) {
        return (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN);
    }

    public static boolean hasAnnotations(ClassNode classNode) {
        return (classNode.visibleAnnotations != null && !classNode.visibleAnnotations.isEmpty())
                || (classNode.invisibleAnnotations != null && !classNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(MethodNode methodNode) {
        return (methodNode.visibleAnnotations != null && !methodNode.visibleAnnotations.isEmpty())
                || (methodNode.invisibleAnnotations != null && !methodNode.invisibleAnnotations.isEmpty());
    }

    public static boolean hasAnnotations(FieldNode fieldNode) {
        return (fieldNode.visibleAnnotations != null && !fieldNode.visibleAnnotations.isEmpty())
                || (fieldNode.invisibleAnnotations != null && !fieldNode.invisibleAnnotations.isEmpty());
    }

    public static boolean isStringInsn(AbstractInsnNode insn) {
        return insn.getOpcode() == Opcodes.LDC && ((LdcInsnNode) insn).cst instanceof String;
    }

    public static boolean isIntInsn(AbstractInsnNode insn) {
        if (insn == null) {
            return false;
        }
        int opcode = insn.getOpcode();
        return ((opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5)
                || opcode == Opcodes.BIPUSH
                || opcode == Opcodes.SIPUSH
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer));
    }

    public static boolean isLongInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode == Opcodes.LCONST_0
                || opcode == Opcodes.LCONST_1
                || (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long));
    }

    public static boolean isFloatInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Float);
    }

    public static boolean isDoubleInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();
        return (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1)
                || (insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Double);
    }

    public static AbstractInsnNode getNumberInsn(int number) {
        if (number >= -1 && number <= 5)
            return new InsnNode(number + 3);
        else if (number >= -128 && number <= 127)
            return new IntInsnNode(Opcodes.BIPUSH, number);
        else if (number >= -32768 && number <= 32767)
            return new IntInsnNode(Opcodes.SIPUSH, number);
        else
            return new LdcInsnNode(number);
    }

    public static AbstractInsnNode getNumberInsn(long number) {
        if (number >= 0 && number <= 1)
            return new InsnNode((int) (number + 9));
        else
            return new LdcInsnNode(number);
    }

    public static AbstractInsnNode getNumberInsn(float number) {
        if (number >= 0 && number <= 2) {
            return new InsnNode((int) (number + 11));
        } else {
            return new LdcInsnNode(number);
        }
    }

    public static AbstractInsnNode getNumberInsn(double number) {
        if (number >= 0 && number <= 1)
            return new InsnNode((int) (number + 14));
        else
            return new LdcInsnNode(number);
    }

    public static String getStringFromInsn(AbstractInsnNode insn) {
        if (isStringInsn(insn)) {
            return (String) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static int getIntegerFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.ICONST_M1 && opcode <= Opcodes.ICONST_5) {
            return opcode - 3;
        } else if (insn instanceof IntInsnNode
                && insn.getOpcode() != Opcodes.NEWARRAY) {
            return ((IntInsnNode) insn).operand;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Integer) {
            return (Integer) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static long getLongFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.LCONST_0 && opcode <= Opcodes.LCONST_1) {
            return opcode - 9;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Long) {
            return (Long) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static float getFloatFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.FCONST_0 && opcode <= Opcodes.FCONST_2) {
            return opcode - 11;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Float) {
            return (Float) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static double getDoubleFromInsn(AbstractInsnNode insn) {
        int opcode = insn.getOpcode();

        if (opcode >= Opcodes.DCONST_0 && opcode <= Opcodes.DCONST_1) {
            return opcode - 14;
        } else if (insn instanceof LdcInsnNode
                && ((LdcInsnNode) insn).cst instanceof Double) {
            return (Double) ((LdcInsnNode) insn).cst;
        }

        throw new RuntimeException("Unexpected instruction");
    }

    public static String getGenericMethodDesc(String desc) {
        Type returnType = Type.getReturnType(desc);
        Type[] args = Type.getArgumentTypes(desc);
        for (int i = 0; i < args.length; i++) {
            Type arg = args[i];

            if (arg.getSort() == Type.OBJECT)
                args[i] = Type.getType("Ljava/lang/Object;");
        }

        return Type.getMethodDescriptor(returnType, args);
    }

    public static int getReturnOpcode(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.IRETURN;
            case Type.FLOAT:
                return Opcodes.FRETURN;
            case Type.LONG:
                return Opcodes.LRETURN;
            case Type.DOUBLE:
                return Opcodes.DRETURN;
            case Type.ARRAY:
            case Type.OBJECT:
                return Opcodes.ARETURN;
            case Type.VOID:
                return Opcodes.RETURN;
            default:
                throw new AssertionError("Unknown type sort: " + type.getClassName());
        }
    }

    public static int getVarOpcode(Type type, boolean store) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return store ? Opcodes.ISTORE : Opcodes.ILOAD;
            case Type.FLOAT:
                return store ? Opcodes.FSTORE : Opcodes.FLOAD;
            case Type.LONG:
                return store ? Opcodes.LSTORE : Opcodes.LLOAD;
            case Type.DOUBLE:
                return store ? Opcodes.DSTORE : Opcodes.DLOAD;
            case Type.ARRAY:
            case Type.OBJECT:
                return store ? Opcodes.ASTORE : Opcodes.ALOAD;
            default:
                throw new AssertionError("Unknown type: " + type.getClassName());
        }
    }

    public static InsnList asList(AbstractInsnNode abstractInsnNode, AbstractInsnNode... abstractInsnNodes) {
        InsnList insnList = new InsnList();
        insnList.add(abstractInsnNode);
        if (abstractInsnNodes != null)
            for (AbstractInsnNode insnNode : abstractInsnNodes)
                insnList.add(insnNode);

        return insnList;
    }

    public static InsnList singletonList(AbstractInsnNode abstractInsnNode) {
        InsnList insnList = new InsnList();
        insnList.add(abstractInsnNode);
        return insnList;
    }

    public static AbstractInsnNode getDefaultValue(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return ASMUtils.getNumberInsn(0);
            case Type.FLOAT:
                return ASMUtils.getNumberInsn(0f);
            case Type.LONG:
                return ASMUtils.getNumberInsn(0L);
            case Type.DOUBLE:
                return ASMUtils.getNumberInsn(0d);
            case Type.OBJECT:
                return new InsnNode(Opcodes.ACONST_NULL);
            default:
                throw new AssertionError();
        }
    }

    public static AbstractInsnNode getRandomValue(Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomInt(0, 2));
            case Type.CHAR:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomInt(Character.MIN_VALUE, Character.MAX_VALUE));
            case Type.BYTE:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomInt(Byte.MIN_VALUE, Byte.MAX_VALUE));
            case Type.SHORT:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomInt(Short.MIN_VALUE, Short.MAX_VALUE));
            case Type.INT:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomInt());
            case Type.FLOAT:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomFloat());
            case Type.LONG:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomLong());
            case Type.DOUBLE:
                return ASMUtils.getNumberInsn(RandomUtils.getRandomDouble());
            case Type.ARRAY:
            case Type.OBJECT:
                return new InsnNode(Opcodes.ACONST_NULL);
            default:
                throw new AssertionError();
        }
    }


    public static List<ClassNode> readClassesWithInputStream(String path) {
        try (InputStream stream = ASMUtils.class.getResourceAsStream(path); ZipInputStream zip = new ZipInputStream(Objects.requireNonNull(stream));) {
            List<ClassNode> list = new ArrayList<>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                    ClassNode cn = new ClassNode();
                    new ClassReader(IOUtils.toByteArray(zip)).accept(cn, 0);
                    list.add(cn);
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeAnnotation(ClassWrapper clazz, String desc) {
        if (clazz.getClassNode().invisibleAnnotations != null) {
            clazz.getClassNode().invisibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
        if (clazz.getClassNode().visibleAnnotations != null) {
            clazz.getClassNode().visibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
    }

    public static void removeAnnotation(MethodWrapper method, String desc) {
        if (method.getMethodNode().invisibleAnnotations != null) {
            method.getMethodNode().invisibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
        if (method.getMethodNode().visibleAnnotations != null) {
            method.getMethodNode().visibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
    }

    public static void removeAnnotation(FieldWrapper field, String desc) {
        if (field.getFieldNode().invisibleAnnotations != null) {
            field.getFieldNode().invisibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
        if (field.getFieldNode().visibleAnnotations != null) {
            field.getFieldNode().visibleAnnotations.removeIf(annotationNode -> annotationNode.desc.equals(desc));
        }
    }

    public static boolean hasAnnotation(ClassWrapper clazz, String desc) {
        if (clazz.getClassNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : clazz.getClassNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        if (clazz.getClassNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : clazz.getClassNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasAnnotation(MethodWrapper method, String desc) {
        if (method.getMethodNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : method.getMethodNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        if (method.getMethodNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : method.getMethodNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean hasAnnotation(FieldWrapper field, String desc) {
        if (field.getFieldNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : field.getFieldNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        if (field.getFieldNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : field.getFieldNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    return true;
                }
            }
        }
        return false;
    }


    public static Map<String, String> getAnnotationValues(ClassWrapper clazz, String desc) {
        Map<String, String> map = new HashMap<>();
        if (clazz.getClassNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : clazz.getClassNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        if (clazz.getClassNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : clazz.getClassNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        return null;
    }

    public static Map<String, String> getAnnotationValues(MethodWrapper method, String desc) {
        Map<String, String> map = new HashMap<>();
        if (method.getMethodNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : method.getMethodNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        if (method.getMethodNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : method.getMethodNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        return null;
    }

    public static InsnList getStringInst(String string) {
        InsnList list = new InsnList();
        if (string.getBytes(StandardCharsets.UTF_8).length > 65535) {
            int end;
            list.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(StringBuilder.class)));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(StringBuilder.class), "<init>", "()V"));
            while (!string.isEmpty()) {
                end = Math.min(string.length(), 65535);
                while (string.substring(0, end).getBytes(StandardCharsets.UTF_8).length > 65535) end--;
                String s = string.substring(0, end);
                string = string.substring(end);
                list.add(new LdcInsnNode(s));
                list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;"));
            }
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getInternalName(StringBuilder.class), "toString", "()Ljava/lang/String;"));
        } else {
            list.add(new LdcInsnNode(string));
        }
        return list;
    }

    public static String toBasicType(Class<?> clz) {
        if (clz == Integer.class) {
            return "I";
        } else if (clz == Long.class) {
            return "J";
        } else if (clz == Float.class) {
            return "F";
        } else if (clz == Double.class) {
            return "D";
        } else if (clz == Short.class) {
            return "S";
        } else if (clz == Byte.class) {
            return "B";
        } else if (clz == Character.class) {
            return "C";
        } else if (clz == Boolean.class) {
            return "Z";
        } else if (clz == Void.class) {
            return "V";
        } else {
            return "L" + clz.getName().replace(".", "/") + ";";
        }
    }


    public static Map<String, String> getAnnotationValues(FieldWrapper field, String desc) {
        Map<String, String> map = new HashMap<>();
        if (field.getFieldNode().invisibleAnnotations != null) {
            for (AnnotationNode annotation : field.getFieldNode().invisibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        if (field.getFieldNode().visibleAnnotations != null) {
            for (AnnotationNode annotation : field.getFieldNode().visibleAnnotations) {
                if (annotation.desc.equals(desc)) {
                    if (annotation.values == null) {
                        return Collections.emptyMap();
                    } else {
                        return StringUtils.createMap(annotation.values.toArray(new Object[0]));
                    }
                }
            }
        }
        return null;
    }


    private static final Map<Integer, String> OPCODE_NAME_MAP = new HashMap<>();

    static {
        OPCODE_NAME_MAP.put(0, "NOP");
        OPCODE_NAME_MAP.put(1, "ACONST_NULL");
        OPCODE_NAME_MAP.put(2, "ICONST_M1");
        OPCODE_NAME_MAP.put(3, "ICONST_0");
        OPCODE_NAME_MAP.put(4, "ICONST_1");
        OPCODE_NAME_MAP.put(5, "ICONST_2");
        OPCODE_NAME_MAP.put(6, "ICONST_3");
        OPCODE_NAME_MAP.put(7, "ICONST_4");
        OPCODE_NAME_MAP.put(8, "ICONST_5");
        OPCODE_NAME_MAP.put(9, "LCONST_0");
        OPCODE_NAME_MAP.put(10, "LCONST_1");
        OPCODE_NAME_MAP.put(11, "FCONST_0");
        OPCODE_NAME_MAP.put(12, "FCONST_1");
        OPCODE_NAME_MAP.put(13, "FCONST_2");
        OPCODE_NAME_MAP.put(14, "DCONST_0");
        OPCODE_NAME_MAP.put(15, "DCONST_1");
        OPCODE_NAME_MAP.put(16, "BIPUSH");
        OPCODE_NAME_MAP.put(17, "SIPUSH");
        OPCODE_NAME_MAP.put(18, "LDC");
        OPCODE_NAME_MAP.put(21, "ILOAD");
        OPCODE_NAME_MAP.put(22, "LLOAD");
        OPCODE_NAME_MAP.put(23, "FLOAD");
        OPCODE_NAME_MAP.put(24, "DLOAD");
        OPCODE_NAME_MAP.put(25, "ALOAD");
        OPCODE_NAME_MAP.put(46, "IALOAD");
        OPCODE_NAME_MAP.put(47, "LALOAD");
        OPCODE_NAME_MAP.put(48, "FALOAD");
        OPCODE_NAME_MAP.put(49, "DALOAD");
        OPCODE_NAME_MAP.put(50, "AALOAD");
        OPCODE_NAME_MAP.put(51, "BALOAD");
        OPCODE_NAME_MAP.put(52, "CALOAD");
        OPCODE_NAME_MAP.put(53, "SALOAD");
        OPCODE_NAME_MAP.put(54, "ISTORE");
        OPCODE_NAME_MAP.put(55, "LSTORE");
        OPCODE_NAME_MAP.put(56, "FSTORE");
        OPCODE_NAME_MAP.put(57, "DSTORE");
        OPCODE_NAME_MAP.put(58, "ASTORE");
        OPCODE_NAME_MAP.put(79, "IASTORE");
        OPCODE_NAME_MAP.put(80, "LASTORE");
        OPCODE_NAME_MAP.put(81, "FASTORE");
        OPCODE_NAME_MAP.put(82, "DASTORE");
        OPCODE_NAME_MAP.put(83, "AASTORE");
        OPCODE_NAME_MAP.put(84, "BASTORE");
        OPCODE_NAME_MAP.put(85, "CASTORE");
        OPCODE_NAME_MAP.put(86, "SASTORE");
        OPCODE_NAME_MAP.put(87, "POP");
        OPCODE_NAME_MAP.put(88, "POP2");
        OPCODE_NAME_MAP.put(89, "DUP");
        OPCODE_NAME_MAP.put(90, "DUP_X1");
        OPCODE_NAME_MAP.put(91, "DUP_X2");
        OPCODE_NAME_MAP.put(92, "DUP2");
        OPCODE_NAME_MAP.put(93, "DUP2_X1");
        OPCODE_NAME_MAP.put(94, "DUP2_X2");
        OPCODE_NAME_MAP.put(95, "SWAP");
        OPCODE_NAME_MAP.put(96, "IADD");
        OPCODE_NAME_MAP.put(97, "LADD");
        OPCODE_NAME_MAP.put(98, "FADD");
        OPCODE_NAME_MAP.put(99, "DADD");
        OPCODE_NAME_MAP.put(100, "ISUB");
        OPCODE_NAME_MAP.put(101, "LSUB");
        OPCODE_NAME_MAP.put(102, "FSUB");
        OPCODE_NAME_MAP.put(103, "DSUB");
        OPCODE_NAME_MAP.put(104, "IMUL");
        OPCODE_NAME_MAP.put(105, "LMUL");
        OPCODE_NAME_MAP.put(106, "FMUL");
        OPCODE_NAME_MAP.put(107, "DMUL");
        OPCODE_NAME_MAP.put(108, "IDIV");
        OPCODE_NAME_MAP.put(109, "LDIV");
        OPCODE_NAME_MAP.put(110, "FDIV");
        OPCODE_NAME_MAP.put(111, "DDIV");
        OPCODE_NAME_MAP.put(112, "IREM");
        OPCODE_NAME_MAP.put(113, "LREM");
        OPCODE_NAME_MAP.put(114, "FREM");
        OPCODE_NAME_MAP.put(115, "DREM");
        OPCODE_NAME_MAP.put(116, "INEG");
        OPCODE_NAME_MAP.put(117, "LNEG");
        OPCODE_NAME_MAP.put(118, "FNEG");
        OPCODE_NAME_MAP.put(119, "DNEG");
        OPCODE_NAME_MAP.put(120, "ISHL");
        OPCODE_NAME_MAP.put(121, "LSHL");
        OPCODE_NAME_MAP.put(122, "ISHR");
        OPCODE_NAME_MAP.put(123, "LSHR");
        OPCODE_NAME_MAP.put(124, "IUSHR");
        OPCODE_NAME_MAP.put(125, "LUSHR");
        OPCODE_NAME_MAP.put(126, "IAND");
        OPCODE_NAME_MAP.put(127, "LAND");
        OPCODE_NAME_MAP.put(128, "IOR");
        OPCODE_NAME_MAP.put(129, "LOR");
        OPCODE_NAME_MAP.put(130, "IXOR");
        OPCODE_NAME_MAP.put(131, "LXOR");
        OPCODE_NAME_MAP.put(132, "IINC");
        OPCODE_NAME_MAP.put(133, "I2L");
        OPCODE_NAME_MAP.put(134, "I2F");
        OPCODE_NAME_MAP.put(135, "I2D");
        OPCODE_NAME_MAP.put(136, "L2I");
        OPCODE_NAME_MAP.put(137, "L2F");
        OPCODE_NAME_MAP.put(138, "L2D");
        OPCODE_NAME_MAP.put(139, "F2I");
        OPCODE_NAME_MAP.put(140, "F2L");
        OPCODE_NAME_MAP.put(141, "F2D");
        OPCODE_NAME_MAP.put(142, "D2I");
        OPCODE_NAME_MAP.put(143, "D2L");
        OPCODE_NAME_MAP.put(144, "D2F");
        OPCODE_NAME_MAP.put(145, "I2B");
        OPCODE_NAME_MAP.put(146, "I2C");
        OPCODE_NAME_MAP.put(147, "I2S");
        OPCODE_NAME_MAP.put(148, "LCMP");
        OPCODE_NAME_MAP.put(149, "FCMPL");
        OPCODE_NAME_MAP.put(150, "FCMPG");
        OPCODE_NAME_MAP.put(151, "DCMPL");
        OPCODE_NAME_MAP.put(152, "DCMPG");
        OPCODE_NAME_MAP.put(153, "IFEQ");
        OPCODE_NAME_MAP.put(154, "IFNE");
        OPCODE_NAME_MAP.put(155, "IFLT");
        OPCODE_NAME_MAP.put(156, "IFGE");
        OPCODE_NAME_MAP.put(157, "IFGT");
        OPCODE_NAME_MAP.put(158, "IFLE");
        OPCODE_NAME_MAP.put(159, "IF_ICMPEQ");
        OPCODE_NAME_MAP.put(160, "IF_ICMPNE");
        OPCODE_NAME_MAP.put(161, "IF_ICMPLT");
        OPCODE_NAME_MAP.put(162, "IF_ICMPGE");
        OPCODE_NAME_MAP.put(163, "IF_ICMPGT");
        OPCODE_NAME_MAP.put(164, "IF_ICMPLE");
        OPCODE_NAME_MAP.put(165, "IF_ACMPEQ");
        OPCODE_NAME_MAP.put(166, "IF_ACMPNE");
        OPCODE_NAME_MAP.put(167, "GOTO");
        OPCODE_NAME_MAP.put(168, "JSR");
        OPCODE_NAME_MAP.put(169, "RET");
        OPCODE_NAME_MAP.put(170, "TABLESWITCH");
        OPCODE_NAME_MAP.put(171, "LOOKUPSWITCH");
        OPCODE_NAME_MAP.put(172, "IRETURN");
        OPCODE_NAME_MAP.put(173, "LRETURN");
        OPCODE_NAME_MAP.put(174, "FRETURN");
        OPCODE_NAME_MAP.put(175, "DRETURN");
        OPCODE_NAME_MAP.put(176, "ARETURN");
        OPCODE_NAME_MAP.put(177, "RETURN");
        OPCODE_NAME_MAP.put(178, "GETSTATIC");
        OPCODE_NAME_MAP.put(179, "PUTSTATIC");
        OPCODE_NAME_MAP.put(180, "GETFIELD");
        OPCODE_NAME_MAP.put(181, "PUTFIELD");
        OPCODE_NAME_MAP.put(182, "INVOKEVIRTUAL");
        OPCODE_NAME_MAP.put(183, "INVOKESPECIAL");
        OPCODE_NAME_MAP.put(184, "INVOKESTATIC");
        OPCODE_NAME_MAP.put(185, "INVOKEINTERFACE");
        OPCODE_NAME_MAP.put(186, "INVOKEDYNAMIC");
        OPCODE_NAME_MAP.put(187, "NEW");
        OPCODE_NAME_MAP.put(188, "NEWARRAY");
        OPCODE_NAME_MAP.put(189, "ANEWARRAY");
        OPCODE_NAME_MAP.put(190, "ARRAYLENGTH");
        OPCODE_NAME_MAP.put(191, "ATHROW");
        OPCODE_NAME_MAP.put(192, "CHECKCAST");
        OPCODE_NAME_MAP.put(193, "INSTANCEOF");
        OPCODE_NAME_MAP.put(194, "MONITORENTER");
        OPCODE_NAME_MAP.put(195, "MONITOREXIT");
        OPCODE_NAME_MAP.put(197, "MULTIANEWARRAY");
        OPCODE_NAME_MAP.put(198, "IFNULL");
        OPCODE_NAME_MAP.put(199, "IFNONNULL");
    }

    public static String getOpcodeString(int opcode) {
        return OPCODE_NAME_MAP.getOrDefault(opcode, "UNKNOWN");
    }

    public static String getOpcodesString(int value, String prefix) {
        for (Field f : Opcodes.class.getFields()) {
            try {
                if (f.getName().startsWith(prefix) && (int) f.get(null) == value) {
                    return f.getName().substring(prefix.length());
                }
            } catch (ReflectiveOperationException e) {
                // ignore
            }
        }
        return null;
    }

    public static boolean getFlag(int value, int flag) {
        return (value & flag) > 0;
    }
}
