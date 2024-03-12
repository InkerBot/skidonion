package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.utils.ASMUtils;

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
                WARN("Advanced module is disabled, some inline methods won't be applied...");
            }
        }
        if (node.name.startsWith("_verification_")) {
            if (verification) {
                context.shouldVirtualize = true;
                compiler.getVirtualizeMacroCount().incrementAndGet();
                processVerification(context, node);
            } else {
                WARN("Verification is disabled, some inline methods won't be applied...");
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
