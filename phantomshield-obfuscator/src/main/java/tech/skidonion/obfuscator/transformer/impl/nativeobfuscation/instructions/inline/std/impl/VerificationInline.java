package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.impl;

import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.transformer.generic.poly.visitors.CVisitor;
import tech.skidonion.obfuscator.transformer.generic.poly.visitors.PublicKeyDecryptorVisitor;
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
            case "tech/skidonion/verification/utils/Internals.initBuffer()V": {
                context.output.append("inlines::__buffer = new jbyte *[").append(context.obfuscator.getVerificationBuffer().size()).append("];\n");
                break;
            }
            case "tech/skidonion/verification/utils/Internals.ed25519verify(Ljava/nio/ByteBuffer;)Z": {
                context.headers.add("\"ed25519.h\"");
                context.output.append("{\n");
//                unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(bufferInstance));
                context.output.append("unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(cstack").append(context.stackPointer - 1).append(".l));\n");
//                unsigned char public_key[32];
                context.output.append("unsigned char public_key[32];\n");
//                unsigned int i_encoded_public_key[32];
                context.output.append("unsigned int i_encoded_public_key[32];\n");
//                for (size_t i = 0; i < 128; i += 4)
                context.output.append("for (size_t i = 0; i < 128; i += 4)\n");
//                {
                context.output.append("{\n");
//                    i_encoded_public_key[i >> 2] = static_cast<unsigned int>(buffer[i]) | static_cast<unsigned int>(buffer[i + 1]) << 8 | static_cast<unsigned int>(buffer[i + 2]) << 16 | static_cast<unsigned int>(buffer[i + 3]) << 24;
                context.output.append("i_encoded_public_key[i >> 2] = static_cast<unsigned int>(buffer[i]) | static_cast<unsigned int>(buffer[i + 1]) << 8 | static_cast<unsigned int>(buffer[i + 2]) << 16 | static_cast<unsigned int>(buffer[i + 3]) << 24;\n");
//                }
                context.output.append("}\n");
//
//                // TODO: poly xor
                context.output.append(new PublicKeyDecryptorVisitor().visit(context.obfuscator.getPolyChainContext())).append("\n");

//                unsigned char *signature = &buffer[128];
                context.output.append("unsigned char *signature = &buffer[128];");
//
//                size_t message_len = static_cast<unsigned int>(buffer[128 + 64]) | static_cast<unsigned int>(buffer[128 + 64 + 1]) << 8 | static_cast<unsigned int>(buffer[128 + 64 + 2]) << 16 | static_cast<unsigned int>(buffer[128 + 64 + 3]) << 24;
                context.output.append("size_t message_len = static_cast<unsigned int>(buffer[128 + 64]) | static_cast<unsigned int>(buffer[128 + 64 + 1]) << 8 | static_cast<unsigned int>(buffer[128 + 64 + 2]) << 16 | static_cast<unsigned int>(buffer[128 + 64 + 3]) << 24;\n");
//
//                static_cast<jint>(ed25519_verify(signature, &buffer[128 + 64 + 4], message_len, public_key));
                context.output.append("cstack").append(context.stackPointer - 1).append(".i = static_cast<jint>(ed25519_verify(signature, &buffer[128 + 64 + 4], message_len, public_key));\n");
//                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("}\n");
                break;
            }
            case "tech/skidonion/verification/utils/Internals.ed25519exchange(Ljava/nio/ByteBuffer;)V": {
                context.headers.add("\"ed25519.h\"");
                context.headers.add("\"sha512.h\"");
                context.output.append("{\n");
//                unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(bufferInstance));
                context.output.append("unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(cstack").append(context.stackPointer - 1).append(".l));\n");

//                unsigned char public_key[32];
                context.output.append("unsigned char public_key[32];\n");
//                unsigned int i_encoded_public_key[32];
                context.output.append("unsigned int i_encoded_public_key[32];\n");
//                for (size_t i = 0; i < 128; i += 4)
                context.output.append("for (size_t i = 0; i < 128; i += 4)\n");
//                {
                context.output.append("{\n");
//                    i_encoded_public_key[i >> 2] = static_cast<unsigned int>(buffer[i]) | static_cast<unsigned int>(buffer[i + 1]) << 8 | static_cast<unsigned int>(buffer[i + 2]) << 16 | static_cast<unsigned int>(buffer[i + 3]) << 24;
                context.output.append("i_encoded_public_key[i >> 2] = static_cast<unsigned int>(buffer[i]) | static_cast<unsigned int>(buffer[i + 1]) << 8 | static_cast<unsigned int>(buffer[i + 2]) << 16 | static_cast<unsigned int>(buffer[i + 3]) << 24;\n");
//                }
                context.output.append("}\n");
//
//                // TODO: poly xor
                context.output.append(new PublicKeyDecryptorVisitor().visit(context.obfuscator.getPolyChainContext())).append("\n");
//
//                ed25519_key_exchange(&buffer[32], public_key, &buffer[128]);
                context.output.append("ed25519_key_exchange(&buffer[32], public_key, &buffer[128]);\n");
//                sha512(&buffer[32], 32, &buffer[32]);
                context.output.append("sha512(&buffer[32], 32, &buffer[32]);\n");
//                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("}\n");
                break;
            }
            case "tech/skidonion/verification/utils/Internals.ed25519generate(Ljava/nio/ByteBuffer;)V": {
                context.headers.add("\"ed25519.h\"");
                context.output.append("{\n");
//                unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(bufferInstance));
                context.output.append("unsigned char *buffer = reinterpret_cast<unsigned char *>(env->GetDirectBufferAddress(cstack").append(context.stackPointer - 1).append(".l));\n");
//                ed25519_create_keypair(&buffer[0], &buffer[32], &buffer[0]);
                context.output.append("ed25519_create_keypair(&buffer[0], &buffer[32], &buffer[0]);\n");
//                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                context.output.append("}\n");
                break;
            }
        }
    }

    @Override
    public String[] methods() {
        return new String[]{
                "tech/skidonion/verification/utils/Internals.initBuffer()V",
                "tech/skidonion/verification/utils/Internals.ed25519verify(Ljava/nio/ByteBuffer;)Z",
                "tech/skidonion/verification/utils/Internals.ed25519exchange(Ljava/nio/ByteBuffer;)V",
                "tech/skidonion/verification/utils/Internals.ed25519generate(Ljava/nio/ByteBuffer;)V",
        };
    }
}
