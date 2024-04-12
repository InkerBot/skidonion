package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;

import java.util.Objects;

import static tech.skidonion.obfuscator.PhantomShield.*;

public class InlineHandler {
    public static void process(MethodContext context, MethodInsnNode node, String trimmedTryCatchBlock) {
        CppCompiler compiler = context.obfuscator.obfuscator.getCompiler();
        boolean verification = context.obfuscator.isVerificationEnable();
        boolean advanced = compiler.isAdvancedModuleEnable();
        if (node.name.startsWith("_advanced_")) {
            if (advanced) {
                context.shouldVirtualize = true;
                compiler.getVirtualizeMacroCount().incrementAndGet();
                processAdvanced(context, node);
            } else {
                WARN(TRANSLATION("phantom-shield-x.native.inline1"));
            }
        } else if (node.name.startsWith("_verification_")) {
            if (verification) {
                context.shouldVirtualize = true;
                compiler.getVirtualizeMacroCount().incrementAndGet();
                processVerification(context, node);
            } else {
                WARN(TRANSLATION("phantom-shield-x.native.inline2"));
            }
        } else if (node.name.startsWith("_field_")) {
            String key = node.name.substring(7);
            Pair<String, FieldWrapper> pair = context.obfuscator.inlineFields.get(key);
            Type returnType = Type.getReturnType(node.desc);
            boolean isSet = returnType.getSort() == Type.VOID;
            FieldWrapper fw = pair.getSecond();
            String desc = fw.getDescription();
            boolean isStatic = fw.getAccess().isStatic();
            int sort = Type.getType(desc).getSort();
            String cname = pair.getFirst();
            if (Objects.equals("(Ljava/lang/Object;)V", node.desc)) {
                if (isStatic) {
                    ERROR(TRANSLATION("phantom-shield-x.native.inline-static-error"));
                    System.exit(0);
                } else {
                    if (sort == Type.ARRAY || sort == Type.OBJECT || sort == Type.METHOD) {
                        context.output.append("{\n");
                        context.output.append("jobject temp = (jobject) inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 1).append(".l];\n");
                        context.output.append("if(!temp) env->DeleteGlobalRef(temp);\n");
                        context.output.append("inlines::").append(cname).append(".erase((uintptr_t)*(void**)cstack").append(context.stackPointer - 1).append(".l);\n");
                        context.output.append("}\n");
                    } else {
                        context.output.append("inlines::").append(cname).append(".erase((uintptr_t)*(void**)cstack").append(context.stackPointer - 1).append(".l);\n");
                    }
                }
            } else {
                switch (sort) {
                    case Type.VOID:
                        throw new UnsupportedOperationException("invalid field desc");
                    case Type.BOOLEAN:
                    case Type.CHAR:
                    case Type.BYTE:
                    case Type.SHORT:
                    case Type.INT:
                        if (isSet) {
                            if (isStatic) {
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".i;\n");
                            } else {
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".i;\n");
                            }
                        } else {
                            if (isStatic) {
                                context.output.append("cstack").append(context.stackPointer).append(".i = (jint) inlines::").append(cname).append(";\n");
                            } else {
                                context.output.append("cstack").append(context.stackPointer - 1).append(".i = (jint) inlines::").append(cname).append("[(uintptr_t)*(void**) cstack").append(context.stackPointer - 1).append(".l];\n");
                            }
                        }
                        break;
                    case Type.FLOAT:
                        if (isSet) {
                            if (isStatic) {
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".f;\n");
                            } else {
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".f;\n");
                            }
                        } else {
                            if (isStatic) {
                                context.output.append("cstack").append(context.stackPointer).append(".f = inlines::").append(cname).append(";\n");
                            } else {
                                context.output.append("cstack").append(context.stackPointer - 1).append(".f = inlines::").append(cname).append("[(uintptr_t)*(void**) cstack").append(context.stackPointer - 1).append(".l];\n");
                            }
                        }
                        break;
                    case Type.LONG:
                        if (isSet) {
                            if (isStatic) {
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".j;\n");
                            } else {
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 3).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".j;\n");
                            }
                        } else {
                            if (isStatic) {
                                context.output.append("cstack").append(context.stackPointer).append(".j = inlines::").append(cname).append(";\n");
                            } else {
                                context.output.append("cstack").append(context.stackPointer - 1).append(".j = inlines::").append(cname).append("[(uintptr_t)*(void**) cstack").append(context.stackPointer - 1).append(".l];\n");
                            }
                        }
                        break;
                    case Type.DOUBLE:
                        if (isSet) {
                            if (isStatic) {
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".d;\n");
                            } else {
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 3).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".d;\n");
                            }
                        } else {
                            if (isStatic) {
                                context.output.append("cstack").append(context.stackPointer).append(".d = inlines::").append(cname).append(";\n");
                            } else {
                                context.output.append("cstack").append(context.stackPointer - 1).append(".d = inlines::").append(cname).append("[(uintptr_t)*(void**) cstack").append(context.stackPointer - 1).append(".l];\n");
                            }
                        }
                        break;
                    case Type.ARRAY:
                    case Type.OBJECT:
                    case Type.METHOD:
                        if (isSet) {
                            if (isStatic) {
                                context.output.append("{\n");
                                context.output.append("jobject temp = (jobject) inlines::").append(cname).append(";\n");
                                context.output.append("if(!temp) env->DeleteGlobalRef(temp);\n");
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("}\n");
                            } else {
                                context.output.append("{\n");
                                context.output.append("jobject temp = (jobject) inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l];\n");
                                context.output.append("if(!temp) env->DeleteGlobalRef(temp);\n");
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("}\n");
                            }
                        } else {
                            if (isStatic) {
                                context.output.append("cstack").append(context.stackPointer).append(".l = (jobject) inlines::").append(cname).append(";\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                            } else {
                                context.output.append("cstack").append(context.stackPointer - 1).append(".l = (jobject) inlines::").append(cname).append("[(uintptr_t)*(void**) cstack").append(context.stackPointer - 1).append(".l];\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                            }
                        }
                        break;
                }
            }
        } else if (Objects.equals("trycatch", node.name)) {
            context.output.append(trimmedTryCatchBlock).append("\n");
        }
    }

    private static void processVerification(MethodContext context, MethodInsnNode node) {
        switch (node.name) {
            case "_verification_checkHardwareID": {
                context.output.append("inlines::CheckHardwareIDValid(env,clazz,(jarray) cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            }
            case "_verification_generateHardwareID": {
                context.output.append("inlines::GenerateHardwareID(env,clazz,(jarray) cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            }
        }
    }

    private static void processAdvanced(MethodContext context, MethodInsnNode node) {
        switch (node.name) {
            case "_advanced_checkProtection": {
                AbstractInsnNode previous = node.getPrevious();
                int constant;
                try {
                    constant = ASMUtils.getIntegerFromInsn(previous);
                } catch (Exception exception) {
                    throw new RuntimeException("Advanced Inline Method need a const argument...");
                }
                context.output.append("CHECK_PROTECTION(cstack").append(context.stackPointer - 1).append(".i,").append(constant).append(")\n");
                break;
            }
            case "_advanced_checkCRCImage": {
                AbstractInsnNode previous = node.getPrevious();
                int constant;
                try {
                    constant = ASMUtils.getIntegerFromInsn(previous);
                } catch (Exception exception) {
                    throw new RuntimeException("Advanced Inline Method need a const argument...");
                }
                context.output.append("CHECK_CODE_INTEGRITY(cstack").append(context.stackPointer - 1).append(".i,").append(constant).append(")\n");

                break;
            }
            case "_advanced_checkIsVirtualPC": {
                AbstractInsnNode previous = node.getPrevious();
                int constant;
                try {
                    constant = ASMUtils.getIntegerFromInsn(previous);
                } catch (Exception exception) {
                    throw new RuntimeException("Advanced Inline Method need a const argument...");
                }
                context.output.append("CHECK_VIRTUAL_PC(cstack").append(context.stackPointer - 1).append(".i,").append(constant).append(")\n");

                break;
            }
            case "_advanced_checkIsDebuggerPresent": {
                AbstractInsnNode previous = node.getPrevious();
                int constant;
                try {
                    constant = ASMUtils.getIntegerFromInsn(previous);
                } catch (Exception exception) {
                    throw new RuntimeException("Advanced Inline Method need a const argument...");
                }
                context.output.append("CHECK_DEBUGGER(cstack").append(context.stackPointer - 1).append(".i,").append(constant).append(")\n");

                break;
            }
        }
    }


}
