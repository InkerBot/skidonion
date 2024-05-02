package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.impl;

import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.AbstractStandardMethodInline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VerificationInline extends AbstractStandardMethodInline {
    @Override
    public void process(String desc, MethodContext context, MethodInsnNode node) {
        switch (desc) {
            case "tech/skidonion/verification/utils/Internals.getNonce()[B":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_a;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setNonce([B)V":
                context.output.append("inlines::__psx_a = (jbyteArray) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getCrypto()Ljava/lang/Object;":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_b;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setCrypto(Ljava/lang/Object;)V":
                context.output.append("inlines::__psx_b = (jobject) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getVerifyToken()Ljava/lang/String;":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_c;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setVerifyToken(Ljava/lang/String;)V":
                context.output.append("inlines::__psx_c = (jobject) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getKey()[B":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_d;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setKey([B)V":
                context.output.append("inlines::__psx_d = (jbyteArray) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getUsername()Ljava/lang/String;":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_e;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setUsername(Ljava/lang/String;)V":
                context.output.append("inlines::__psx_e = (jobject) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getUserId()J":
                context.output.append("cstack").append(context.stackPointer).append(".j = ").append("(jlong) inlines::__psx_f;").append("\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setUserId(J)V":
                context.output.append("inlines::__psx_f = (jlong) cstack").append(context.stackPointer - 2).append(".j;\n");
                break;
            case "tech/skidonion/verification/utils/Internals.getMagicKey()[B":
                context.output.append("cstack").append(context.stackPointer).append(".l = ").append("(jobject) inlines::__psx_g;").append("\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.setMagicKey([B)V":
                context.output.append("inlines::__psx_g = (jbyteArray) env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                break;
            case "tech/skidonion/verification/utils/Internals.initBuffer()V": {
                context.output.append("inlines::__buffer = new jbyte *[").append(context.obfuscator.getVerificationBuffer().size()).append("];\n");
                break;
            }
        }
    }

    @Override
    public String[] methods() {
        return new String[]{
                "tech/skidonion/verification/utils/Internals.initBuffer()V",
                "tech/skidonion/verification/utils/Internals.getNonce()[B",
                "tech/skidonion/verification/utils/Internals.setNonce([B)V",
                "tech/skidonion/verification/utils/Internals.getCrypto()Ljava/lang/Object;",
                "tech/skidonion/verification/utils/Internals.setCrypto(Ljava/lang/Object;)V",
                "tech/skidonion/verification/utils/Internals.getVerifyToken()Ljava/lang/String;",
                "tech/skidonion/verification/utils/Internals.setVerifyToken(Ljava/lang/String;)V",
                "tech/skidonion/verification/utils/Internals.getKey()[B",
                "tech/skidonion/verification/utils/Internals.setKey([B)V",
                "tech/skidonion/verification/utils/Internals.getUsername()Ljava/lang/String;",
                "tech/skidonion/verification/utils/Internals.setUsername(Ljava/lang/String;)V",
                "tech/skidonion/verification/utils/Internals.getUserId()J",
                "tech/skidonion/verification/utils/Internals.setUserId(J)V",
                "tech/skidonion/verification/utils/Internals.getMagicKey()[B",
                "tech/skidonion/verification/utils/Internals.setMagicKey([B)V",
        };
    }
}
