package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class IndyPreprocessor implements Preprocessor {

    private static void processIndy(NativeObfuscation obfuscation, ClassNode classNode, MethodNode methodNode,
                                    InvokeDynamicInsnNode invokeDynamicInsnNode) {
        LabelNode bootstrapStart = new LabelNode();
        LabelNode bootstrapEnd = new LabelNode();
        LabelNode bsmeStart = new LabelNode();
        LabelNode invokeStart = new LabelNode();


        LabelNode isCachedCallSiteStart = new LabelNode();
        InsnList checkIsCallSiteCachedInstructions = new InsnList();
        checkIsCallSiteCachedInstructions.add(isCachedCallSiteStart);
        checkIsCallSiteCachedInstructions.add(PreprocessorUtils.GET_CALLSITE.get());
        checkIsCallSiteCachedInstructions.add(new JumpInsnNode(Opcodes.IFNULL, bootstrapStart));
        checkIsCallSiteCachedInstructions.add(new JumpInsnNode(Opcodes.GOTO, invokeStart));

        LabelNode prepareArgumentsStart = new LabelNode();
        InsnList prepareArgumentsInstructions = new InsnList();
        prepareArgumentsInstructions.add(prepareArgumentsStart);
        Type[] arguments = Type.getArgumentTypes(invokeDynamicInsnNode.desc);

        prepareArgumentsInstructions.add(new LdcInsnNode(arguments.length)); // 1
        prepareArgumentsInstructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object")); // 1
        {
            int index = arguments.length;
            for (Type argument : IOUtils.reverse(Arrays.stream(arguments)).collect(Collectors.toList())) {
                index--;
                if (argument.getSize() == 1) {
                    if (argument.getSort() != Type.ARRAY && argument.getSort() != Type.OBJECT) {
                        prepareArgumentsInstructions.add(new InsnNode(Opcodes.SWAP)); // 2
                        prepareArgumentsInstructions.add(getBoxingInsnNode(argument)); // 2
                        prepareArgumentsInstructions.add(new InsnNode(Opcodes.SWAP)); // 2
                    }
                } else if (argument.getSize() == 2) {
                    prepareArgumentsInstructions.add(new InsnNode(Opcodes.DUP_X2)); // 3
                    prepareArgumentsInstructions.add(new InsnNode(Opcodes.POP)); // 2
                    prepareArgumentsInstructions.add(getBoxingInsnNode(argument)); // 2
                    prepareArgumentsInstructions.add(new InsnNode(Opcodes.SWAP)); // 2
                }
                prepareArgumentsInstructions.add(new InsnNode(Opcodes.DUP)); // 3
                prepareArgumentsInstructions.add(new InsnNode(Opcodes.DUP2_X1)); // 5
                prepareArgumentsInstructions.add(new InsnNode(Opcodes.POP2)); // 3
                prepareArgumentsInstructions.add(new LdcInsnNode(index)); // 4
                prepareArgumentsInstructions.add(new InsnNode(Opcodes.SWAP)); // 4
                prepareArgumentsInstructions.add(new InsnNode(Opcodes.AASTORE)); // 1
            }
        }

        InsnList bootstrapInstructions = new InsnList();
        bootstrapInstructions.add(bootstrapStart); // 1


        Type[] bsmArguments = Type.getArgumentTypes(invokeDynamicInsnNode.bsm.getDesc());
        int targetArgLength = bsmArguments.length - 3;
        int originArgLength = invokeDynamicInsnNode.bsmArgs.length;

        // process variable arguments for bsm like StringConcatFactory.makeConcatWithConstants(Lookup, String, MethodType, String, Object...)
        // jvm will process variable argument automatically when using linkCallSite
        // but if we want to use invokeWithArguments, we need to process variable argument manually
        if (originArgLength < targetArgLength) {
            Object[] newArgs = new Object[targetArgLength];
            System.arraycopy(invokeDynamicInsnNode.bsmArgs, 0, newArgs, 0, originArgLength);

            if (targetArgLength - originArgLength != 1)
                throw new RuntimeException("Impossible BootstrapMethod Arguments Length");

            if (bsmArguments[originArgLength + 3].getSort() == Type.ARRAY) {
                newArgs[originArgLength] = new Object[0];
            } else {
                throw new RuntimeException("Last Argument of BootstrapMethod is NOT a Variable Argument");
            }

            invokeDynamicInsnNode.bsmArgs = newArgs;
        } else if (originArgLength > targetArgLength || (bsmArguments[bsmArguments.length - 1].getSort() == Type.ARRAY && Type.getType(invokeDynamicInsnNode.bsmArgs[invokeDynamicInsnNode.bsmArgs.length - 1].getClass()).getSort() != Type.ARRAY)) {
            Object[] newArgs = new Object[targetArgLength];
            System.arraycopy(invokeDynamicInsnNode.bsmArgs, 0, newArgs, 0, targetArgLength - 1);

            Object[] varArgs = new Object[originArgLength - targetArgLength + 1];
            System.arraycopy(invokeDynamicInsnNode.bsmArgs, targetArgLength - 1, varArgs, 0, originArgLength - targetArgLength + 1);

            newArgs[targetArgLength - 1] = varArgs;
            invokeDynamicInsnNode.bsmArgs = newArgs;
        }


        bootstrapInstructions.add(PreprocessorUtils.LOOKUP_LOCAL.get()); // 2
        bootstrapInstructions.add(new LdcInsnNode(invokeDynamicInsnNode.name)); // 3
        bootstrapInstructions.add(MethodHandleUtils.generateMethodTypeLdcInsn(Type.getMethodType(invokeDynamicInsnNode.desc)));

        for (Object bsmArgument : invokeDynamicInsnNode.bsmArgs) {
            if (bsmArgument instanceof String) {
                bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Type) {
                if (((Type) bsmArgument).getSort() == Type.METHOD) {
                    bootstrapInstructions.add(MethodHandleUtils.generateMethodTypeLdcInsn((Type) bsmArgument));
                } else {
                    bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 5
                }
            } else if (bsmArgument instanceof Integer) {
                bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Long) {
                bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 6
            } else if (bsmArgument instanceof Float) {
                bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Double) {
                bootstrapInstructions.add(new LdcInsnNode(bsmArgument)); // 6
            } else if (bsmArgument instanceof Handle) {
                bootstrapInstructions.add(MethodHandleUtils.generateMethodHandleLdcInsn((Handle) bsmArgument));
            } else if (bsmArgument instanceof Object[]) {
                Object[] objects = (Object[]) bsmArgument;
                bootstrapInstructions.add(new LdcInsnNode(objects.length));
                bootstrapInstructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

                int index = 0;
                for (Object object : objects) {
                    bootstrapInstructions.add(new InsnNode(Opcodes.DUP));
                    bootstrapInstructions.add(new LdcInsnNode(index));
                    if (object instanceof String) {
                        bootstrapInstructions.add(new LdcInsnNode(object));
                    } else if (object instanceof Type) {
                        if (((Type) object).getSort() == Type.METHOD) {
                            bootstrapInstructions.add(MethodHandleUtils.generateMethodTypeLdcInsn((Type) object));
                        } else {
                            bootstrapInstructions.add(new LdcInsnNode(object));
                        }
                    } else if (object instanceof Integer) {
                        bootstrapInstructions.add(new LdcInsnNode(object));
                        bootstrapInstructions.add(getBoxingInsnNode(Type.INT_TYPE));
                    } else if (object instanceof Long) {
                        bootstrapInstructions.add(new LdcInsnNode(object));
                        bootstrapInstructions.add(getBoxingInsnNode(Type.LONG_TYPE));
                    } else if (object instanceof Float) {
                        bootstrapInstructions.add(new LdcInsnNode(object));
                        bootstrapInstructions.add(getBoxingInsnNode(Type.FLOAT_TYPE));
                    } else if (object instanceof Double) {
                        bootstrapInstructions.add(new LdcInsnNode(object));
                        bootstrapInstructions.add(getBoxingInsnNode(Type.DOUBLE_TYPE));
                    } else if (object instanceof Handle) {
                        bootstrapInstructions.add(MethodHandleUtils.generateMethodHandleLdcInsn((Handle) object));
                    } else {
                        throw new RuntimeException("Wrong argument type: " + object.getClass());
                    }
                    bootstrapInstructions.add(new InsnNode(Opcodes.AASTORE));
                    index++;
                }

            } else {
                throw new RuntimeException("Wrong argument type: " + bsmArgument.getClass());
            }
        }
        bootstrapInstructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, invokeDynamicInsnNode.bsm.getOwner(),
                invokeDynamicInsnNode.bsm.getName(), invokeDynamicInsnNode.bsm.getDesc())); // 2
        bootstrapInstructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/invoke/CallSite")); // 2
        bootstrapInstructions.add(PreprocessorUtils.CACHE_CALLSITE.get()); // 1
        bootstrapInstructions.add(new JumpInsnNode(Opcodes.GOTO, invokeStart)); // 1
        bootstrapInstructions.add(bootstrapEnd);

        InsnList invokeInstructions = new InsnList();
        invokeInstructions.add(invokeStart);
        invokeInstructions.add(PreprocessorUtils.GET_CALLSITE_AND_INCREMENT.get()); // 2
        invokeInstructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/lang/invoke/CallSite",
                "getTarget", "()Ljava/lang/invoke/MethodHandle;")); // 2
        invokeInstructions.add(new InsnNode(Opcodes.SWAP)); // 2
        invokeInstructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
                "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;")); // 1
        Type returnType = Type.getReturnType(invokeDynamicInsnNode.desc);
        if (returnType.getSort() == Type.OBJECT) {
            invokeInstructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName())); // 1
        } else if (returnType.getSort() == Type.ARRAY) {
            invokeInstructions.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getDescriptor())); // 1
        } else {
            invokeInstructions.add(getUnboxingTypeInsn(returnType));
        }

        InsnList bsmeInstructions = new InsnList();
        bsmeInstructions.add(bsmeStart); // 1
        bsmeInstructions.add(new InsnNode(Opcodes.DUP));
        bsmeInstructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/BootstrapMethodError"));
        LabelNode throwLabel = new LabelNode();
        bsmeInstructions.add(new JumpInsnNode(Opcodes.IFNE, throwLabel));
        bsmeInstructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/BootstrapMethodError")); // 2
        bsmeInstructions.add(new InsnNode(Opcodes.DUP)); // 3
        bsmeInstructions.add(new InsnNode(Opcodes.DUP2_X1)); // 5
        bsmeInstructions.add(new InsnNode(Opcodes.POP2)); // 3
        bsmeInstructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/BootstrapMethodError",
                "<init>", "(Ljava/lang/Throwable;)V")); // 1
        bsmeInstructions.add(throwLabel);
        bsmeInstructions.add(new InsnNode(Opcodes.ATHROW)); // 0

        InsnList resultInstructions = new InsnList();
        resultInstructions.add(prepareArgumentsInstructions);
        resultInstructions.add(checkIsCallSiteCachedInstructions);
        resultInstructions.add(bootstrapInstructions);
        resultInstructions.add(bsmeInstructions);
        resultInstructions.add(invokeInstructions);

        methodNode.instructions.insert(invokeDynamicInsnNode, resultInstructions);
        methodNode.instructions.remove(invokeDynamicInsnNode);
        methodNode.tryCatchBlocks.add(0, new TryCatchBlockNode(bootstrapStart, bootstrapEnd, bsmeStart, "java/lang/Throwable"));
    }


    private static AbstractInsnNode getBoxingInsnNode(Type argument) {
        switch (argument.getSort()) {
            case Type.BOOLEAN:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;");
            case Type.BYTE:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;");
            case Type.CHAR:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;");
            case Type.DOUBLE:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;");
            case Type.FLOAT:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;");
            case Type.INT:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;");
            case Type.LONG:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
            case Type.SHORT:
                return new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;");
            default:
                throw new RuntimeException(String.format("Failed to box %s", argument));
        }
    }

    private static InsnList getUnboxingTypeInsn(Type argument) {
        InsnList result = new InsnList();
        switch (argument.getSort()) {
            case Type.BOOLEAN:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"));
                break;
            case Type.BYTE:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B"));
                break;
            case Type.CHAR:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C"));
                break;
            case Type.DOUBLE:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D"));
                break;
            case Type.FLOAT:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F"));
                break;
            case Type.INT:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I"));
                break;
            case Type.LONG:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J"));
                break;
            case Type.SHORT:
                result.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
                result.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S"));
                break;
            case Type.VOID:
                result.add(new InsnNode(Opcodes.POP));
                break;
            default:
                throw new RuntimeException(String.format("Failed to unbox %s", argument));
        }
        return result;
    }

    @Override
    public void process(NativeObfuscation obfuscation, ClassNode classNode, MethodNode methodNode) {
        for (int i = 0; i < methodNode.instructions.size(); i++) {
            AbstractInsnNode insnNode = methodNode.instructions.get(i);
            if (insnNode instanceof InvokeDynamicInsnNode) {
                processIndy(obfuscation, classNode, methodNode, (InvokeDynamicInsnNode) insnNode);
            }
        }
    }
}
