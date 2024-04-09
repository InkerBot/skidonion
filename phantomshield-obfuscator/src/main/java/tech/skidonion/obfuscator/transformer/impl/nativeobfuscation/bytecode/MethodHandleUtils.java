package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ListIterator;

public class MethodHandleUtils {

    private static AbstractInsnNode getTypeLoadInsnNode(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
            case Type.OBJECT:
                return new LdcInsnNode(type);
            case Type.BOOLEAN:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Boolean", "TYPE", "Ljava/lang/Class;");
            case Type.BYTE:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Byte", "TYPE", "Ljava/lang/Class;");
            case Type.CHAR:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Character", "TYPE", "Ljava/lang/Class;");
            case Type.DOUBLE:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Double", "TYPE", "Ljava/lang/Class;");
            case Type.FLOAT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Float", "TYPE", "Ljava/lang/Class;");
            case Type.INT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Integer", "TYPE", "Ljava/lang/Class;");
            case Type.LONG:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Long", "TYPE", "Ljava/lang/Class;");
            case Type.SHORT:
                return new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/Short", "TYPE", "Ljava/lang/Class;");
            default:
                throw new RuntimeException(String.format("Unsupported TypeLoad type: %s", type));
        }
    }

    public static void generateMethodTypeLdcInsn(Type type, ListIterator<AbstractInsnNode> iterator) {
        if (type.getSort() != Type.METHOD) {
            throw new RuntimeException(String.format("Not a MT: %s", type));
        }
        iterator.add(new LdcInsnNode(type.getDescriptor())); // 5
        iterator.add(PreprocessorUtils.CLASSLOADER_LOCAL.get()); // 6
        iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType",
                "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")); // 5
    }

    public static void generateMethodHandleLdcInsn(Handle handle, ListIterator<AbstractInsnNode> iterator) {
        iterator.add(PreprocessorUtils.LOOKUP_LOCAL.get()); // 5
        iterator.add(new LdcInsnNode(Type.getObjectType(handle.getOwner()))); // 6
        switch (handle.getTag()) {
            case Opcodes.H_GETFIELD:
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTFIELD:
            case Opcodes.H_PUTSTATIC:
                iterator.add(new LdcInsnNode(handle.getName())); // 7
                iterator.add(getTypeLoadInsnNode(Type.getType(handle.getDesc()))); // 8
                String methodName = "";
                switch (handle.getTag()) {
                    case Opcodes.H_GETFIELD:
                        methodName = "findGetter";
                        break;
                    case Opcodes.H_GETSTATIC:
                        methodName = "findStaticGetter";
                        break;
                    case Opcodes.H_PUTFIELD:
                        methodName = "findSetter";
                        break;
                    case Opcodes.H_PUTSTATIC:
                        methodName = "findStaticSetter";
                        break;
                }
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/invoke/MethodHandles$Lookup", methodName,
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;")); // 5
                break;
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKEINTERFACE:
                iterator.add(new LdcInsnNode(handle.getName())); // 7
                iterator.add(new LdcInsnNode(handle.getDesc())); // 8
                iterator.add(PreprocessorUtils.CLASSLOADER_LOCAL.get()); // 9
                iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType",
                        "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")); // 8
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, // 5
                        "java/lang/invoke/MethodHandles$Lookup", "findVirtual",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"));
                break;
            case Opcodes.H_INVOKESTATIC:
                iterator.add(new LdcInsnNode(handle.getName())); // 7
                iterator.add(new LdcInsnNode(handle.getDesc())); // 8
                iterator.add(PreprocessorUtils.CLASSLOADER_LOCAL.get()); // 9
                iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType",
                        "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")); // 8
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, // 5
                        "java/lang/invoke/MethodHandles$Lookup", "findStatic",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"));
                break;
            case Opcodes.H_INVOKESPECIAL:
                iterator.add(new LdcInsnNode(handle.getName())); // 7
                iterator.add(new LdcInsnNode(handle.getDesc())); // 8
                iterator.add(PreprocessorUtils.CLASSLOADER_LOCAL.get()); // 9
                iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType",
                        "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")); // 8
                iterator.add(PreprocessorUtils.CLASS_LOCAL.get()); // 9
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, // 5
                        "java/lang/invoke/MethodHandles$Lookup", "findSpecial",
                        "(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;"));
                break;
            case Opcodes.H_NEWINVOKESPECIAL:
                iterator.add(new LdcInsnNode(handle.getDesc())); // 7
                iterator.add(PreprocessorUtils.CLASSLOADER_LOCAL.get()); // 8
                iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/invoke/MethodType",
                        "fromMethodDescriptorString", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;")); // 7
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, // 5
                        "java/lang/invoke/MethodHandles$Lookup", "findConstructor",
                        "(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;"));
                break;
        }
    }
}
