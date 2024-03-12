package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.impl;

import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.AbstractInlineMethodBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.impl.verify.VerificationConstants;

import java.util.ArrayList;
import java.util.List;

public class VerificationInlineBuilder extends AbstractInlineMethodBuilder {
    public VerificationInlineBuilder(CppCompiler compiler) {
        super(compiler);
        compiler.getVirtualizeMacroCount().addAndGet(3);
    }

    @Override
    public String[] injectHeader() {
        List<String> headers = new ArrayList<>();
        if (compiler.isAdvancedModuleEnable()) {
            headers.add("\"ThemidaSDK.h\"");
        } else {
            headers.add("\"VirtualizerSDK.h\"");
        }
        return headers.toArray(new String[0]);
    }

    @Override
    public String buildCpp() {
        cpp.append(VerificationConstants.getPool());
        cpp.append("char *get_pool() {\nreturn pool;\n}\n");

        cpp.append("char *string_pool;\n");

        cpp.append("jstring cstrings[29];\n");
        cpp.append("std::mutex cclasses_mtx[24];\n");
        cpp.append("jclass cclasses[24];\n");
        cpp.append("jmethodID cmethods[45];\n");
        cpp.append("jfieldID cfields[1];\n");

        cpp.append("void __init(JNIEnv *env){\n").append(vmStart()).append("volatile bool __dummy = true;\nif(__dummy){\n").append(VerificationConstants.init()).append("}\n").append(vmEnd()).append("}\n");
        cpp.append("void CheckHardwareIDValid(JNIEnv *env, jclass clazz, jarray arg0) {\n").append(vmStart()).append("volatile bool __dummy = true;\nif(__dummy){\n").append(VerificationConstants.checkHardwareIDValid()).append("}\n").append(vmEnd()).append("}\n");
        cpp.append("void GenerateHardwareID(JNIEnv *env, jclass clazz, jarray arg0) {\n").append(vmStart()).append("volatile bool __dummy = true;\nif(__dummy){\n").append(VerificationConstants.generateHardwareID()).append("}\n").append(vmEnd()).append("}\n");

        return cpp.toString();
    }

    @Override
    public String buildHpp() {
//        hpp.append("char *get_pool();\n");
        hpp.append("void __init(JNIEnv *env);\n");
        hpp.append("void CheckHardwareIDValid(JNIEnv *env, jclass clazz, jarray arg0);\n");
        hpp.append("void GenerateHardwareID(JNIEnv *env, jclass clazz, jarray arg0);\n");
        return hpp.toString();
    }
}
