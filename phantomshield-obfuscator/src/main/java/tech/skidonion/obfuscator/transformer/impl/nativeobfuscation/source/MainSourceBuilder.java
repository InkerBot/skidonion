package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

public class MainSourceBuilder {

    private final Set<String> includes;
    private final StringBuilder registerMethods;
    private final StringBuilder codes;

    public MainSourceBuilder() {
        includes = new HashSet<>();
        registerMethods = new StringBuilder();
        codes = new StringBuilder();
    }

    public void addHeader(String header) {
        includes.add(header);
    }

    public void addHeaders(Set<String> headers) {
        includes.addAll(headers);
    }

    public void registerClassMethods(int classId, String escapedClassName) {
        registerMethods.append(String.format(
                "        reg_methods[%d] = &(native_jvm::classes::__ngen_%s::__ngen_register_methods);\n",
                classId, escapedClassName));
    }

    public void registerDefine(String stringPooledClassName, String classFileName) {
        registerMethods.append(String.format(
                "        env->DeleteLocalRef(env->DefineClass(%s, nullptr, native_jvm::data::__ngen_%s::get_class_data(), native_jvm::data::__ngen_%s::get_class_data_length()));\n",
                stringPooledClassName,
                classFileName,
                classFileName
        ));
    }

    public void addConvertedCode(String code) {
        codes.append(code).append("\n\n");
    }

    public void addAdditionCode(String code) {
        registerMethods.append(code);
    }

    public String build(String nativeDir, int classCount) {
        StringBuilder includesBuilder = new StringBuilder();

        for (String include : includes) {
            includesBuilder.append(String.format("#include %s\n", include));
        }

        String template = FileUtils.readResource("sources/native_jvm_output.cpp");
        return StringUtils.dynamicFormat(template, StringUtils.createStringMap(
                "register_code", registerMethods,
                "includes", includesBuilder,
                "native_dir", nativeDir,
                "class_count", classCount,
                "converted_codes", codes
        ));
    }
}
