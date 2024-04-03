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

import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;
import static tech.skidonion.obfuscator.PhantomShield.WARN;

public class InlineHandler {
    public static void process(MethodContext context, MethodInsnNode node) {
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
        }
        if (node.name.startsWith("_verification_")) {
            if (verification) {
                context.shouldVirtualize = true;
                compiler.getVirtualizeMacroCount().incrementAndGet();
                processVerification(context, node);
            } else {
                WARN(TRANSLATION("phantom-shield-x.native.inline2"));
            }
        }

        if (node.name.startsWith("_field_")) {
            String key = node.name.substring(7);
            Pair<String, FieldWrapper> pair = context.obfuscator.inlineStaticFields.get(key);
            Type returnType = Type.getReturnType(node.desc);
            boolean isSet = returnType.getSort() == Type.VOID;

            String desc = pair.getSecond().getDescription();
            int sort = Type.getType(desc).getSort();
            switch (sort) {
                case Type.VOID:
                    throw new UnsupportedOperationException("invalid field desc");
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".i;\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".i = (jint) inlines::").append(pair.getFirst()).append(";\n");
                    }
                    break;
                case Type.FLOAT:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 1).append(".f;\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".f = inlines::").append(pair.getFirst()).append(";\n");
                    }
                    break;
                case Type.LONG:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".j;\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".j = inlines::").append(pair.getFirst()).append(";\n");
                    }
                    break;
                case Type.DOUBLE:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") cstack").append(context.stackPointer - 2).append(".d;\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".d = inlines::").append(pair.getFirst()).append(";\n");
                    }
                    break;
                case Type.ARRAY:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                        context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".l = (jobject) inlines::").append(pair.getFirst()).append(";\n");
                        context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                    }
                    break;
                case Type.OBJECT:
                case Type.METHOD:
                    if (isSet) {
                        context.output.append("inlines::").append(pair.getFirst()).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                        context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                    } else {
                        context.output.append("cstack").append(context.stackPointer).append(".l = inlines::").append(pair.getFirst()).append(";\n");
                        context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                    }
                    break;
            }
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
