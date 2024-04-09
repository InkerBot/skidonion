package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.util.Arrays;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class PreprocessorRunner {

    public static void preprocess(MethodNode methodNode) {

        ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode insnNode = iterator.next();
            if (insnNode instanceof LdcInsnNode) {
                LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;

                if (ldcInsnNode.cst instanceof Handle) {
                    iterator.remove();
                    MethodHandleUtils.generateMethodHandleLdcInsn((Handle) ldcInsnNode.cst, iterator);
                } else if (ldcInsnNode.cst instanceof Type) {
                    Type type = (Type) ldcInsnNode.cst;

                    if (type.getSort() == Type.METHOD) {
                        iterator.remove();
                        MethodHandleUtils.generateMethodTypeLdcInsn(type, iterator);
                    }
                }
            } else if (insnNode instanceof InvokeDynamicInsnNode) {
                iterator.remove();
                processIndy(methodNode, (InvokeDynamicInsnNode) insnNode, iterator);
            }
        }
    }

    private static void processIndy(MethodNode methodNode, InvokeDynamicInsnNode invokeDynamicInsnNode, ListIterator<AbstractInsnNode> iterator) {
        LabelNode bootstrapStart = new LabelNode();
        LabelNode bootstrapEnd = new LabelNode();
        LabelNode bsmeStart = new LabelNode();
        LabelNode invokeStart = new LabelNode();


        // prepare
        LabelNode prepareArgumentsStart = new LabelNode();
        iterator.add(prepareArgumentsStart);
        Type[] arguments = Type.getArgumentTypes(invokeDynamicInsnNode.desc);

        iterator.add(new LdcInsnNode(arguments.length)); // 1
        iterator.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object")); // 1
        {
            int index = arguments.length;
            for (Type argument : IOUtils.reverse(Arrays.stream(arguments)).collect(Collectors.toList())) {
                index--;
                if (argument.getSize() == 1) {
                    if (argument.getSort() != Type.ARRAY && argument.getSort() != Type.OBJECT) {
                        iterator.add(new InsnNode(Opcodes.SWAP)); // 2
                        iterator.add(getBoxingInsnNode(argument)); // 2
                        iterator.add(new InsnNode(Opcodes.SWAP)); // 2
                    }
                } else if (argument.getSize() == 2) {
                    iterator.add(new InsnNode(Opcodes.DUP_X2)); // 3
                    iterator.add(new InsnNode(Opcodes.POP)); // 2
                    iterator.add(getBoxingInsnNode(argument)); // 2
                    iterator.add(new InsnNode(Opcodes.SWAP)); // 2
                }
                iterator.add(new InsnNode(Opcodes.DUP)); // 3
                iterator.add(new InsnNode(Opcodes.DUP2_X1)); // 5
                iterator.add(new InsnNode(Opcodes.POP2)); // 3
                iterator.add(new LdcInsnNode(index)); // 4
                iterator.add(new InsnNode(Opcodes.SWAP)); // 4
                iterator.add(new InsnNode(Opcodes.AASTORE)); // 1
            }
        }

        // get call site
        LabelNode isCachedCallSiteStart = new LabelNode();
        iterator.add(isCachedCallSiteStart);
        iterator.add(PreprocessorUtils.GET_CALLSITE.get());
        iterator.add(new JumpInsnNode(Opcodes.IFNULL, bootstrapStart));
        iterator.add(new JumpInsnNode(Opcodes.GOTO, invokeStart));


        // bsm
        iterator.add(bootstrapStart); // 1


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


        iterator.add(PreprocessorUtils.LOOKUP_LOCAL.get()); // 2
        iterator.add(new LdcInsnNode(invokeDynamicInsnNode.name)); // 3
        MethodHandleUtils.generateMethodTypeLdcInsn(Type.getMethodType(invokeDynamicInsnNode.desc), iterator);

        for (Object bsmArgument : invokeDynamicInsnNode.bsmArgs) {
            if (bsmArgument instanceof String) {
                iterator.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Type) {
                if (((Type) bsmArgument).getSort() == Type.METHOD) {
                    MethodHandleUtils.generateMethodTypeLdcInsn((Type) bsmArgument, iterator);
                } else {
                    iterator.add(new LdcInsnNode(bsmArgument)); // 5
                }
            } else if (bsmArgument instanceof Integer) {
                iterator.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Long) {
                iterator.add(new LdcInsnNode(bsmArgument)); // 6
            } else if (bsmArgument instanceof Float) {
                iterator.add(new LdcInsnNode(bsmArgument)); // 5
            } else if (bsmArgument instanceof Double) {
                iterator.add(new LdcInsnNode(bsmArgument)); // 6
            } else if (bsmArgument instanceof Handle) {
                MethodHandleUtils.generateMethodHandleLdcInsn((Handle) bsmArgument, iterator);
            } else if (bsmArgument instanceof Object[]) {
                Object[] objects = (Object[]) bsmArgument;
                iterator.add(new LdcInsnNode(objects.length));
                iterator.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

                int index = 0;
                for (Object object : objects) {
                    iterator.add(new InsnNode(Opcodes.DUP));
                    iterator.add(new LdcInsnNode(index));
                    if (object instanceof String) {
                        iterator.add(new LdcInsnNode(object));
                    } else if (object instanceof Type) {
                        if (((Type) object).getSort() == Type.METHOD) {
                            MethodHandleUtils.generateMethodTypeLdcInsn((Type) object, iterator);
                        } else {
                            iterator.add(new LdcInsnNode(object));
                        }
                    } else if (object instanceof Integer) {
                        iterator.add(new LdcInsnNode(object));
                        iterator.add(getBoxingInsnNode(Type.INT_TYPE));
                    } else if (object instanceof Long) {
                        iterator.add(new LdcInsnNode(object));
                        iterator.add(getBoxingInsnNode(Type.LONG_TYPE));
                    } else if (object instanceof Float) {
                        iterator.add(new LdcInsnNode(object));
                        iterator.add(getBoxingInsnNode(Type.FLOAT_TYPE));
                    } else if (object instanceof Double) {
                        iterator.add(new LdcInsnNode(object));
                        iterator.add(getBoxingInsnNode(Type.DOUBLE_TYPE));
                    } else if (object instanceof Handle) {
                        MethodHandleUtils.generateMethodHandleLdcInsn((Handle) object, iterator);
                    } else {
                        throw new RuntimeException("Wrong argument type: " + object.getClass());
                    }
                    iterator.add(new InsnNode(Opcodes.AASTORE));
                    index++;
                }

            } else {
                throw new RuntimeException("Wrong argument type: " + bsmArgument.getClass());
            }
        }
        iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, invokeDynamicInsnNode.bsm.getOwner(),
                invokeDynamicInsnNode.bsm.getName(), invokeDynamicInsnNode.bsm.getDesc())); // 2
        iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/invoke/CallSite")); // 2
        iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "trycatch", "()V", false));
        iterator.add(PreprocessorUtils.CACHE_CALLSITE.get()); // 1
        iterator.add(new JumpInsnNode(Opcodes.GOTO, invokeStart)); // 1
        iterator.add(bootstrapEnd);

        // bsm exception

        iterator.add(bsmeStart); // 1
        iterator.add(new InsnNode(Opcodes.DUP));
        iterator.add(new TypeInsnNode(Opcodes.INSTANCEOF, "java/lang/BootstrapMethodError"));
        LabelNode throwLabel = new LabelNode();
        iterator.add(new JumpInsnNode(Opcodes.IFNE, throwLabel));
        iterator.add(new TypeInsnNode(Opcodes.NEW, "java/lang/BootstrapMethodError")); // 2
        iterator.add(new InsnNode(Opcodes.DUP)); // 3
        iterator.add(new InsnNode(Opcodes.DUP2_X1)); // 5
        iterator.add(new InsnNode(Opcodes.POP2)); // 3
        iterator.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/BootstrapMethodError",
                "<init>", "(Ljava/lang/Throwable;)V")); // 1
        iterator.add(throwLabel);
        iterator.add(new InsnNode(Opcodes.ATHROW)); // 0

        // invoke

        iterator.add(invokeStart);
        iterator.add(PreprocessorUtils.GET_CALLSITE_AND_INCREMENT.get()); // 2
        iterator.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/lang/invoke/CallSite",
                "getTarget", "()Ljava/lang/invoke/MethodHandle;")); // 2
        iterator.add(new InsnNode(Opcodes.SWAP)); // 2
        iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/invoke/MethodHandle",
                "invokeWithArguments", "([Ljava/lang/Object;)Ljava/lang/Object;")); // 1
        Type returnType = Type.getReturnType(invokeDynamicInsnNode.desc);
        if (returnType.getSort() == Type.OBJECT) {
            iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName())); // 1
        } else if (returnType.getSort() == Type.ARRAY) {
            iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getDescriptor())); // 1
        } else {
            getUnboxingTypeInsn(returnType, iterator);
        }
        iterator.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "trycatch", "()V", false));

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

    private static void getUnboxingTypeInsn(Type argument, ListIterator<AbstractInsnNode> iterator) {
        switch (argument.getSort()) {
            case Type.BOOLEAN:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z"));
                break;
            case Type.BYTE:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Byte", "byteValue", "()B"));
                break;
            case Type.CHAR:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C"));
                break;
            case Type.DOUBLE:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Double", "doubleValue", "()D"));
                break;
            case Type.FLOAT:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Float", "floatValue", "()F"));
                break;
            case Type.INT:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I"));
                break;
            case Type.LONG:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Long", "longValue", "()J"));
                break;
            case Type.SHORT:
                iterator.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
                iterator.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Short", "shortValue", "()S"));
                break;
            case Type.VOID:
                iterator.add(new InsnNode(Opcodes.POP));
                break;
            default:
                throw new RuntimeException(String.format("Failed to unbox %s", argument));
        }
    }
}
