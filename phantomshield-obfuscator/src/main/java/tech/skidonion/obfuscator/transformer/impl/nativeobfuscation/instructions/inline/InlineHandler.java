package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification.BufferContext;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.utils.commons.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static tech.skidonion.obfuscator.PhantomShield.*;

public class InlineHandler {
    @NativeObfuscation(verificationLock = "基础用户组")
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
        } else if (node.name.startsWith("_field_")) {
            boolean isGarbageCollector;
            String key = node.name.substring(7);
            if (key.startsWith("-")) {
                key = key.substring(1);
                isGarbageCollector = true;
            } else {
                isGarbageCollector = false;
            }

            Pair<String, FieldWrapper> pair = context.obfuscator.inlineFields.get(key);
            FieldWrapper fw = pair.getSecond();
            String cname = pair.getFirst();

            boolean isSet = Type.getReturnType(node.desc).getSort() == Type.VOID;
            boolean isStatic = fw.getAccess().isStatic();
            int sort = Type.getType(fw.getDescription()).getSort();
            if (isGarbageCollector && Objects.equals("(Ljava/lang/Object;)V", node.desc)) {
                if (!isStatic) {
                    if (sort == Type.ARRAY || sort == Type.OBJECT || sort == Type.METHOD) {
                        context.output.append("env->DeleteGlobalRef((jobject) inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 1).append(".l]);\n");
                        context.output.append("inlines::").append(cname).append(".erase((uintptr_t)*(void**)cstack").append(context.stackPointer - 1).append(".l);\n");
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
                                context.output.append("env->DeleteGlobalRef((jobject) inlines::").append(cname).append(");\n");
                                context.output.append("inlines::").append(cname).append(" = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
                            } else {
                                context.output.append("env->DeleteGlobalRef((jobject) inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l]);\n");
                                context.output.append("inlines::").append(cname).append("[(uintptr_t)*(void**)cstack").append(context.stackPointer - 2).append(".l] = (").append(MethodProcessor.CPP_TYPES[sort]).append(") env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);\n");
                                context.output.append("refs.insert(cstack").append(context.stackPointer - 1).append(".l);\n");
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
        } else if (node.name.startsWith("_method_")) {
            String key = node.name.substring(8);
            Pair<String, MethodWrapper> pair = context.obfuscator.inlineMethods.get(key);
            String cname = pair.getFirst();

            boolean isStatic = node.getOpcode() == Opcodes.INVOKESTATIC;

            Type returnType = Type.getReturnType(node.desc);
            Type[] args = Type.getArgumentTypes(node.desc);

            StringBuilder argsBuilder = new StringBuilder();
            List<Integer> argOffsets = new ArrayList<>();

            int stackOffset = context.stackPointer;
            for (Type argType : args) {
                stackOffset -= argType.getSize();
            }
            int argumentOffset = stackOffset;

//            Type[] _args = new Type[args.length];
//            System.arraycopy(args, 1, _args, 0, args.length - 1);
//            _args[args.length - 1] = args[0];

            for (Type argType : args) {
                argOffsets.add(argumentOffset);
                argumentOffset += argType.getSize();
            }

            int objectOffset = isStatic ? 0 : 1;
            int argSize = argOffsets.size() - (isStatic ? 1 : 0);

            if (isStatic) {
                argsBuilder.append(", (jclass) ").append(context.getSnippets().getSnippet("INVOKE_ARG_" + args[argSize].getSort(),
                        StringUtils.createStringMap("index", argOffsets.get(argSize))));
            }

            for (int i = 0; i < argSize; i++) {
                argsBuilder.append(", ");
                argsBuilder.append("(").append(MethodProcessor.CPP_TYPES[args[i].getSort()]).append(")").append(context.getSnippets().getSnippet("INVOKE_ARG_" + args[i].getSort(),
                        StringUtils.createStringMap("index", argOffsets.get(i))));
            }

            int returnStackIndex = stackOffset - objectOffset;

            switch (returnType.getSort()) {
                case Type.VOID:
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append(");\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
                case Type.BOOLEAN:
                case Type.CHAR:
                case Type.BYTE:
                case Type.SHORT:
                case Type.INT:
                    // cstack$returnstackindex.i = (jint)
                    context.output.append("cstack").append(returnStackIndex).append(".i = (jint) ");
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append(");\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
                case Type.FLOAT:
                    context.output.append("cstack").append(returnStackIndex).append(".f = ");
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append(");\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
                case Type.LONG:
                    context.output.append("cstack").append(returnStackIndex).append(".j = ");
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append(");\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
                case Type.DOUBLE:
                    context.output.append("cstack").append(returnStackIndex).append(".d = ");
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append(");\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
                case Type.ARRAY:
                case Type.OBJECT:
                case Type.METHOD:
                    context.output.append("cstack").append(returnStackIndex).append(".l = ");
                    context.output.append("inlines::").append(cname).append("(env").append(argsBuilder).append("); refs.insert(cstack").append(returnStackIndex).append(".l);\n");
                    if (!context.manualTryCatch) {
                        context.output.append(trimmedTryCatchBlock);
                    }
                    break;
            }
        } else if (node.name.startsWith("_encrypt_")) {
            String key = node.name.substring(9);
            ClassWrapper cw = context.obfuscator.obfuscator.getClassWrapper(key);
            String classFileName = "data_" + StringUtils.escapeCppNameString(cw.getName().replace('/', '_'));
            context.output.append("{ jbyteArray src = env->NewByteArray(native_jvm::data::__ngen_")
                    .append(classFileName).append("::get_class_data_length()); ")
                    .append("env->SetByteArrayRegion(src, 0, native_jvm::data::__ngen_").append(classFileName)
                    .append("::get_class_data_length(), native_jvm::data::__ngen_").append(classFileName)
                    .append("::get_class_data()); cstack")
                    .append(context.stackPointer)
                    .append(".l = (jobject) src; refs.insert(cstack").append(context.stackPointer)
                    .append(".l); }\n");
//            {
//                jbyteArray src = env->NewByteArray(native_jvm::data::__ngen_%s::get_class_data_length());
//                env->SetByteArrayRegion(src, 0, native_jvm::data::__ngen_%s::get_class_data_length(), native_jvm::data::__ngen_%s::get_class_data());
//            }
//            sb.append("ALOAD=cstack$stackindex0.l = clocal$var.l; refs.insert(cstack$stackindex0.l);\n");
        } else if (node.name.startsWith("_defineClass_")) {
            String key = node.name.substring(13);
            ClassWrapper cw = context.obfuscator.obfuscator.getClassWrapper(key);
            context.output.append("{ ")
                    .append("jbyte* src = new jbyte[").append("cstack").append(context.stackPointer - 1).append(".i]; ")
                    .append("env->GetByteArrayRegion((jbyteArray) ").append("cstack").append(context.stackPointer - 2).append(".l, 0, cstack")
                    .append(context.stackPointer - 1).append(".i, src); ")
                    .append("env->DeleteLocalRef(env->DefineClass(")
                    .append(context.getStringPool().get(cw.getName()))
                    .append(", classloader, src, cstack").append(context.stackPointer - 1).append(".i)); delete src; }\n");

//            jbyte* src = new jbyte[length];
//            env->GetByteArrayRegion((jbyteArray) dst, 0, length, src);
//
//            env->DeleteLocalRef(env->DefineClass(((char *)(string_pool + 67LL)), nullptr, src, length));
//            delete src;
        } else if (node.name.startsWith("_decryptBuffer_")) {
            String key = node.name.substring(15);
            BufferContext bufferContext = context.obfuscator.getVerificationBuffer().get(key);
            if (bufferContext == null) {
                ERROR(TRANSLATION("phantom-shield-x.native.role-error"), key);
                System.exit(0);
                return;
            }

            byte[] dst = bufferContext.getEncryptedBuffer();
            List<Byte> data = new ArrayList<>(dst.length);
            for (byte b : dst) {
                data.add(b);
            }

//                {
            context.output.append("{ jbyte *key = new jbyte[32]; env->GetByteArrayRegion((jbyteArray)cstack").append(context.stackPointer - 1)
                    .append(".l, 0, 32, key); jbyte s[256] = {0}; jbyte *data = new jbyte[256]{")
                    .append(data.stream().map(String::valueOf).collect(Collectors.joining(", ")))
                    .append(" }; { int i = 0, j = 0; jbyte k[256] = {0}; jbyte tmp = 0; for (i = 0; i < 256; i++) { ")
                    .append("s[i] = static_cast<jbyte>(i); k[i] = key[i % 32]; } ")
                    .append("for (i = 0; i < 256; i++) { j = (j + s[i] + k[i]) & 0xFF; tmp = s[i]; tmp = s[i]; s[i] = s[j]; s[j] = tmp; } } ")
                    .append("{ int i = 0, j = 0, t = 0; jbyte tmp; for (int k = 0; k < 256; k++) {")
                    .append("j = (j + s[i]) & 0xFF; tmp = s[i]; s[i] = s[j]; s[j] = tmp; t = (s[i] + s[j]) & 0xFF; data[k] = static_cast<jbyte>(data[k] ^ static_cast<int>(s[t])); ")
                    .append("i = (i + 1) & 0xFF; } } inlines::__buffer[").append(bufferContext.getIndex()).append("] = data; }\n");


            // debug
//            context.output.append("printf(\"[\"); for (int i = 0; i < 256; i++) { printf(\"%d, \", inlines::__buffer[").append(bufferContext.getIndex()).append("][i]); ")
//                    .append("} printf(\"\\b\\b]\\n\");\n");

        } else if (Objects.equals("trycatch", node.name)) {
            context.output.append(trimmedTryCatchBlock).append("\n");
        } else if (Objects.equals("processEnvironment", node.name)) {
            int size = context.obfuscator.getVerificationBuffer().size();

            context.output.append("if(inlines::__buffer) { ");
            for (int i = 0; i < size; i++) {
                context.output.append("if(inlines::__buffer[").append(i).append("])").append("delete[] inlines::__buffer[").append(i).append("]; ");
            }
            context.output.append(" delete[] inlines::__buffer; inlines::__buffer = nullptr; } \n");

//            if (rand_field)
//            {
//                if (rand_field[0])
//                    delete[] rand_field[0];
//                if (rand_field[1])
//                    delete[] rand_field[1];
//                if (rand_field[2])
//                    delete[] rand_field[2];
//                delete[] rand_field;
//                rand_field = nullptr;
//            }
        }
    }

    @NativeObfuscation(verificationLock = "基础用户组")
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
