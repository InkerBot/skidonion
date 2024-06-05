package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import org.objectweb.asm.Type;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.NodeCache;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class InlineSourceBuilder {

    private final NativeObfuscation obfuscation;
    private final CppCompiler compiler;
    private final StringBuilder cpp = new StringBuilder();
    private final StringBuilder hpp = new StringBuilder();

    private final StringPool stringPool;
    private final String prefixVM;

    public InlineSourceBuilder(NativeObfuscation obfuscation, CppCompiler compiler, StringPool stringPool) {
        this.obfuscation = obfuscation;
        this.compiler = compiler;
        this.stringPool = stringPool;

        this.prefixVM = compiler.isAdvancedModuleEnable() ? "VM" : "VIRTUALIZER";
    }


    public void buildHeader(Set<String> headers, boolean virtualize) {
        // cpp
        cpp.append("#include \"native_jvm.hpp\"\n");
        cpp.append("#include \"native_jvm_inline.hpp\"\n");
        cpp.append("#include \"string_pool.hpp\"\n");
        cpp.append("#include <unordered_map>\n");

        for (String header : headers) {
            cpp.append("#include ").append(header).append("\n");
        }

        if (virtualize) {
            if (compiler.isAdvancedModuleEnable()) {
                cpp.append("#include \"ThemidaSDK.h\"\n");
            } else {
                cpp.append("#include \"VirtualizerSDK.h\"\n");
            }
        }


        // hpp
        hpp.append("#include \"native_jvm.hpp\"\n");
        hpp.append("#include <unordered_map>\n");
        hpp.append("#ifndef NATIVE_JVM_INLINE_HPP_GUARD\n");
        hpp.append("#define NATIVE_JVM_INLINE_HPP_GUARD\n");

    }

    public void buildEncryptedClasses(StringBuilder encryptedClassBuilder) {

        cpp.append(encryptedClassBuilder);

        cpp.append("namespace native_jvm::inlines {\n");


        hpp.append("namespace native_jvm::inlines {\n");
    }

    public void buildInlineFields() {
        obfuscation.inlineFields.forEach(((key, pair) -> {
            String cppName = pair.getFirst();
            FieldWrapper fw = pair.getSecond();
            String ctype = MethodProcessor.CPP_TYPES[Type.getType(fw.getDescription()).getSort()];
            if (fw.getAccess().isStatic()) {
                cpp.append("    ").append(ctype).append(" ").append(cppName).append(";\n");
                hpp.append("    extern ").append(ctype).append(" ").append(cppName).append(";\n");
            } else {
                cpp.append("    std::unordered_map<uintptr_t, ")
                        .append(ctype)
                        .append("> ")
                        .append(cppName)
                        .append(";\n");
                hpp.append("    extern ").append("std::unordered_map<uintptr_t, ")
                        .append(ctype)
                        .append("> ")
                        .append(cppName)
                        .append(";\n");
                //std::unordered_map<uintptr_t, int> map;
            }
        }));
    }

    public void buildInlineMethods(String instructions, String declarations, NodeCache<String> strings, int classes, int methods, int fields, int callsites, boolean virtualize) {

        cpp.append("    char *string_pool;\n\n");

        if (!strings.isEmpty()) {
            cpp.append(String.format("    jstring cstrings[%d];\n", strings.size()));
        }
        if (classes > 0) {
            cpp.append(String.format("    std::mutex cclasses_mtx[%d];\n", classes));
            cpp.append(String.format("    jclass cclasses[%d] = {};\n", classes));

            cpp.append("    bool _CacheClass0(JNIEnv *env, jobject classloader, int class_index , int class_name_index){if (!cclasses[class_index]){cclasses_mtx[class_index].lock();if (!cclasses[class_index]){if (jclass clazz = utils::find_class_wo_static(env, classloader, (cstrings[class_name_index]))){cclasses[class_index] = (jclass)env->NewGlobalRef(clazz);env->DeleteLocalRef(clazz);}}cclasses_mtx[class_index].unlock();return true;}return false;}\n");
            cpp.append("    bool _CacheClass1(JNIEnv *env, int class_index, long long offset){if (!cclasses[class_index]){cclasses_mtx[class_index].lock();if (!cclasses[class_index]){if (jclass clazz = env->FindClass((char *)(string_pool + offset))){cclasses[class_index] = (jclass)env->NewGlobalRef(clazz);env->DeleteLocalRef(clazz);}}cclasses_mtx[class_index].unlock();return true;}return false;}\n");
        }
        if (methods > 0) {
            cpp.append(String.format("    jmethodID cmethods[%d];\n", methods));
            cpp.append("    bool _CacheMethod(JNIEnv *env, int class_id, int method_index, long long name_offset, long long desc_offset){if (!cmethods[method_index]){cmethods[method_index] = env->GetMethodID((cclasses[class_id]), ((char *)(string_pool + name_offset)), ((char *)(string_pool + desc_offset)));return true;}return false;}\n");
            cpp.append("    bool _CacheStaticMethod(JNIEnv *env, int class_id, int method_index, long long name_offset, long long desc_offset){if (!cmethods[method_index]){cmethods[method_index] = env->GetStaticMethodID((cclasses[class_id]), ((char *)(string_pool + name_offset)), ((char *)(string_pool + desc_offset)));return true;}return false;}\n");
        }
        if (fields > 0) {
            cpp.append(String.format("    jfieldID cfields[%d];\n", fields));
            cpp.append("    bool _CacheField(JNIEnv *env, int class_id, int field_index, long long name_offset, long long desc_offset){if (!cfields[field_index]){cfields[field_index] = env->GetFieldID((cclasses[class_id]), ((char *)(string_pool + name_offset)), ((char *)(string_pool + desc_offset)));return true;}return false;}\n");
            cpp.append("    bool _CacheStaticField(JNIEnv *env, int class_id, int field_index, long long name_offset, long long desc_offset){if (!cfields[field_index]){cfields[field_index] = env->GetStaticFieldID((cclasses[class_id]), ((char *)(string_pool + name_offset)), ((char *)(string_pool + desc_offset)));return true;}return false;}\n");
        }

        if (callsites > 0) {
            cpp.append(String.format("    jobject ccallsites[%d];\n", callsites));
        }

        cpp.append("\n");
        cpp.append("    ");

        cpp.append(instructions).append("\n");

        cpp.append("    void init(JNIEnv *env) {\n");
        if (virtualize) cpp.append(vmStart());
        cpp.append("        string_pool = string_pool::get_pool();\n\n");

        for (Map.Entry<String, Integer> string : strings.getCache().entrySet()) {
            cpp.append("        if (jstring str = env->NewStringUTF(").append(stringPool.get(string.getKey())).append(")) { if (jstring int_str = utils::get_interned(env, str)) { ")
                    .append(String.format("cstrings[%d] = ", string.getValue()))
                    .append("(jstring) env->NewGlobalRef(int_str); env->DeleteLocalRef(str); env->DeleteLocalRef(int_str); } }\n");
        }

        if (virtualize) cpp.append(vmEnd());

        cpp.append("    }\n");

        hpp.append("    void init(JNIEnv *env);\n");
        hpp.append(declarations);
    }

    public void buildVerificationField() {
        if (obfuscation.isVerificationEnable()) {
            if (obfuscation.isUseInternalVerificationInterface()) {
                cpp.append("bool* licenced = new bool(false);\n");
                hpp.append("extern bool* licenced;\n");
            }
            cpp.append("jbyte** __buffer;\n");
            cpp.append("jbyteArray __psx_a;\n");
            cpp.append("jobject __psx_b;\n");
            cpp.append("jobject __psx_c;\n");
            cpp.append("jbyteArray __psx_d;\n");
            cpp.append("jobject __psx_e;\n");
            cpp.append("jlong __psx_f;\n");
            cpp.append("jbyteArray __psx_g;\n");

            hpp.append("extern jbyte** __buffer;\n");
//            hpp.append("extern jbyteArray nonce;\n");
            hpp.append("extern jbyteArray __psx_a;\n");
//            hpp.append("extern jobject crypto;\n");
            hpp.append("extern jobject __psx_b;\n");
//            hpp.append("extern jobject verify_token;\n");
            hpp.append("extern jobject __psx_c;\n");
//            hpp.append("extern jbyteArray key;\n");
            hpp.append("extern jbyteArray __psx_d;\n");
//            hpp.append("extern jobject username;\n");
            hpp.append("extern jobject __psx_e;\n");
//            hpp.append("extern jlong user_id;\n");
            hpp.append("extern jlong __psx_f;\n");
//            hpp.append("extern jbyteArray magic_key;\n");
            hpp.append("extern jbyteArray __psx_g;\n");

        }

    }

    public void buildTail() {
        cpp.append("}\n");

        hpp.append("}\n");
        hpp.append("#endif\n");
    }


    public String buildCpp() {
        return cpp.toString();
    }

    public String buildHpp() {
        return hpp.toString();
    }

    protected String vmStart() {
        return prefixVM + "_TIGER_WHITE_START\n";
    }

    protected String vmEnd() {
        return prefixVM + "_TIGER_WHITE_END\n";
    }

}
