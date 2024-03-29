package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenCppMethod;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.NodeCache;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ClassSourceBuilder implements AutoCloseable {
    private final NativeObfuscation obfuscation;
    private final CppCompiler compiler;
    private final Path cppFile;
    private final Path hppFile;
    private final BufferedWriter cppWriter;
    private final BufferedWriter hppWriter;
    private final String className;
    private final String filename;

    private final StringPool stringPool;
    private final String prefixVM;

    public ClassSourceBuilder(NativeObfuscation obfuscation, Path cppOutputDir, String className, int classIndex, StringPool stringPool) throws IOException {
        this.obfuscation = obfuscation;
        this.compiler = obfuscation.obfuscator.getCompiler();
        this.className = className;
        this.stringPool = stringPool;

        this.prefixVM = compiler.isAdvancedModuleEnable() ? "VM" : "VIRTUALIZER";
        filename = String.format("%s_%d", StringUtils.escapeCppNameString(className.replace('/', '_')), classIndex);

        cppFile = cppOutputDir.resolve(filename.concat(".cpp"));
        hppFile = cppOutputDir.resolve(filename.concat(".hpp"));
        cppWriter = Files.newBufferedWriter(cppFile, StandardCharsets.UTF_8);
        hppWriter = Files.newBufferedWriter(hppFile, StandardCharsets.UTF_8);
    }

    public void addHeader(Set<String> headers, int strings, int classes, int methods, int fields, int callsites, boolean virtualize) throws IOException {
        for (String header : headers) {
            cppWriter.append("#include ").append(header).append("\n");
        }

        cppWriter.append("#include \"../native_jvm.hpp\"\n");
        cppWriter.append("#include \"../string_pool.hpp\"\n");
        cppWriter.append("#include \"../native_jvm_inline.hpp\"\n");
        if (virtualize) {
            if (compiler.isAdvancedModuleEnable()) {
                cppWriter.append("#include \"../ThemidaSDK.h\"\n");
            } else {
                cppWriter.append("#include \"../VirtualizerSDK.h\"\n");
            }
        }
        cppWriter.append("#include \"").append(getHppFilename()).append("\"\n");
        cppWriter.append("\n");
        cppWriter.append("// ").append(StringUtils.escapeCommentString(className)).append("\n");
        cppWriter.append("namespace native_jvm::classes::__ngen_").append(filename).append(" {\n\n");
        cppWriter.append("    char *string_pool;\n\n");

        if (strings > 0) {
            cppWriter.append(String.format("    jstring cstrings[%d];\n", strings));
        }
        if (classes > 0) {
            cppWriter.append(String.format("    std::mutex cclasses_mtx[%d];\n", classes));
            cppWriter.append(String.format("    jclass cclasses[%d];\n", classes));
        }
        if (methods > 0) {
            cppWriter.append(String.format("    jmethodID cmethods[%d];\n", methods));
        }
        if (fields > 0) {
            cppWriter.append(String.format("    jfieldID cfields[%d];\n", fields));
        }

        if (callsites > 0) {
            cppWriter.append(String.format("    jobject ccallsites[%d];\n", callsites));
        }

        cppWriter.append("\n");
        cppWriter.append("    ");


        hppWriter.append("#include \"../native_jvm.hpp\"\n");
        hppWriter.append("\n");
        hppWriter.append("#ifndef ").append(filename.concat("_hpp").toUpperCase()).append("_GUARD\n");
        hppWriter.append("\n");
        hppWriter.append("#define ").append(filename.concat("_hpp").toUpperCase()).append("_GUARD\n");
        hppWriter.append("\n");
        hppWriter.append("// ").append(StringUtils.escapeCommentString(className)).append("\n");
        hppWriter.append("namespace native_jvm::classes::__ngen_")
                .append(filename)
                .append(" {\n\n");
    }

    public void addInstructions(String instructions) throws IOException {
        cppWriter.append(instructions);
        cppWriter.append("\n");
    }

    public void registerMethods(NodeCache<String> strings, NodeCache<String> classes, String
            nativeMethods, List<HiddenCppMethod> hiddenMethods, boolean virtualize, boolean internal) throws IOException {

        cppWriter.append("    void __ngen_register_methods(JNIEnv *env, jclass clazz) {\n");
        if (virtualize) {
            compiler.getVirtualizeMacroCount().addAndGet(3);
            cppWriter.append(vmStart());
        }

        if (obfuscation.isVerificationEnable() && !internal && obfuscation.isUseInternalVerificationInterface()) {
            cppWriter.append("if(!inlines::licenced){\n");
            cppWriter.append("jclass _auth_class = env->FindClass(\"");
            cppWriter.append(obfuscation.obfuscator.getClassWrapper("tech/skidonion/verification/Main").getName());//
            cppWriter.append("\");\n");
            cppWriter.append("if (env->ExceptionCheck())return;\n");
            cppWriter.append("jclass auth_class = (jclass) env->NewGlobalRef(_auth_class);\n");
            cppWriter.append("env->DeleteLocalRef(_auth_class);\n");
            cppWriter.append("jmethodID show_verification_id = env->GetStaticMethodID(auth_class, \"");
            MethodWrapper show_verification = obfuscation.injectedWrapperMethods.get("tech/skidonion/verification/Main.showVerification()I");
            cppWriter.append(show_verification.getName());
            cppWriter.append("\", \"");
            cppWriter.append(show_verification.getDescription());
            cppWriter.append("\");\n");
            cppWriter.append("if (env->ExceptionCheck())return;\n");
            cppWriter.append("inlines::licenced = env->CallStaticIntMethod(auth_class ,show_verification_id);\n");
            cppWriter.append("if(!inlines::licenced)exit(0);\n");
            cppWriter.append("}\n");
        }

        cppWriter.append("        string_pool = string_pool::get_pool();\n\n");

        for (Map.Entry<String, Integer> string : strings.getCache().entrySet()) {
            cppWriter.append("        if (jstring str = env->NewStringUTF(").append(stringPool.get(string.getKey())).append(")) { if (jstring int_str = utils::get_interned(env, str)) { ")
                    .append(String.format("cstrings[%d] = ", string.getValue()))
                    .append("(jstring) env->NewGlobalRef(int_str); env->DeleteLocalRef(str); env->DeleteLocalRef(int_str); } }\n");
        }

        if (!classes.isEmpty()) {
            cppWriter.append("\n");
        }

        if (!nativeMethods.isEmpty()) {
            cppWriter.append("        JNINativeMethod __ngen_methods[] = {\n");
            cppWriter.append(nativeMethods);
            cppWriter.append("        };\n\n");
            cppWriter.append("        if (clazz) env->RegisterNatives(clazz, __ngen_methods, sizeof(__ngen_methods) / sizeof(__ngen_methods[0]));\n");
            cppWriter.append("        if (env->ExceptionCheck()) { fprintf(stderr, \"Exception occured while registering native_jvm for %s\\n\", ")
                    .append(stringPool.get(className.replace('/', '.')))
                    .append("); fflush(stderr); env->ExceptionDescribe(); env->ExceptionClear(); }\n");
            cppWriter.append("\n");
        }

        if (!hiddenMethods.isEmpty()) {
            HashMap<ClassNode, List<HiddenCppMethod>> sortedHiddenMethods = new HashMap<>();
            for (HiddenCppMethod method : hiddenMethods) {
                sortedHiddenMethods.computeIfAbsent(method.getHiddenMethod().getClassNode(), unused -> new ArrayList<>()).add(method);
            }

            for (ClassNode hiddenClazz : sortedHiddenMethods.keySet()) {
                cppWriter.append("        {\n");
                cppWriter.append("            jclass hidden_class = env->FindClass(").append(stringPool.get(hiddenClazz.name)).append(");\n");
                cppWriter.append("            JNINativeMethod __ngen_hidden_methods[] = {\n");
                for (HiddenCppMethod method : sortedHiddenMethods.get(hiddenClazz)) {
                    cppWriter.append(String.format("                { %s, %s, (void *)&%s },\n",
                            stringPool.get(method.getHiddenMethod().getMethodNode().name),
                            stringPool.get(method.getHiddenMethod().getMethodNode().desc),
                            method.getCppName()));
                }
                cppWriter.append("            };\n");
                cppWriter.append("            if (hidden_class) env->RegisterNatives(hidden_class, __ngen_hidden_methods, sizeof(__ngen_hidden_methods) / sizeof(__ngen_hidden_methods[0]));\n");
                cppWriter.append("            if (env->ExceptionCheck()) { fprintf(stderr, \"Exception occured while registering native_jvm for %s\\n\", ")
                        .append(stringPool.get(hiddenClazz.name.replace('/', '.')))
                        .append("); fflush(stderr); env->ExceptionDescribe(); env->ExceptionClear(); }\n");
                cppWriter.append("            env->DeleteLocalRef(hidden_class);\n");
                cppWriter.append("        }\n");

            }
        }

        if (virtualize) cppWriter.append(vmEnd());

        cppWriter.append("    }\n");
        cppWriter.append("}");

        hppWriter.append("    void __ngen_register_methods(JNIEnv *env, jclass clazz);\n");
        hppWriter.append("}\n\n#endif");
    }

    public String getFilename() {
        return filename;
    }

    public String getHppFilename() {
        return hppFile.getFileName().toString();
    }

    public String getCppFilename() {
        return cppFile.getFileName().toString();
    }

    public Path getCppFile() {
        return cppFile;
    }

    protected String vmStart() {
        return prefixVM + "_DOLPHIN_WHITE_START\n";
    }

    protected String vmEnd() {
        return prefixVM + "_DOLPHIN_WHITE_END\n";
    }

    @Override
    public void close() throws IOException {
        try {
            cppWriter.close();
        } finally {
            hppWriter.close();
        }
    }
}
