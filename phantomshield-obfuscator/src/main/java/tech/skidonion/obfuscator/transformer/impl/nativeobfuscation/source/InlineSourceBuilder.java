package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.impl.VerificationInlineBuilder;

import java.util.*;

public class InlineSourceBuilder {
    private final NativeObfuscation obfuscation;
    private final CppCompiler compiler;
    private final StringBuilder cpp = new StringBuilder();
    private final StringBuilder hpp = new StringBuilder();

    private final List<AbstractInlineMethodBuilder> inlinesInjector;


    public InlineSourceBuilder(NativeObfuscation obfuscation, CppCompiler compiler) {
        this.obfuscation = obfuscation;
        this.compiler = compiler;
        this.inlinesInjector = new ArrayList<AbstractInlineMethodBuilder>() {
            {
                if (obfuscation.isVerificationEnable()) {
                    add(new VerificationInlineBuilder(compiler));
                }
            }
        };
    }


    public String buildCpp() {
        cpp.append("#include \"native_jvm.hpp\"\n");
        cpp.append("#include \"native_jvm_inline.hpp\"\n");
        Set<String> headers = new HashSet<>();
        for (AbstractInlineMethodBuilder abstractInlineMethodBuilder : this.inlinesInjector) {
            String[] header = abstractInlineMethodBuilder.injectHeader();
            if (header != null) {
                headers.addAll(Arrays.asList(header));
            }
        }
        for (String header : headers) {
            cpp.append("#include ").append(header).append("\n");
        }
        cpp.append("namespace native_jvm::inlines {\n");

        if (obfuscation.isVerificationEnable() && obfuscation.isUseInternalVerificationInterface()) {
            cpp.append("bool licenced = 0;\n");
        }

        for (AbstractInlineMethodBuilder abstractInlineMethodBuilder : this.inlinesInjector) {
            String method = abstractInlineMethodBuilder.buildCpp();
            if (method != null) cpp.append(method);
        }

        cpp.append("}\n");
        return cpp.toString();
    }

    public String buildHpp() {
        hpp.append("#include \"native_jvm.hpp\"\n");
        hpp.append("#ifndef NATIVE_JVM_INLINE_HPP_GUARD\n");
        hpp.append("#define NATIVE_JVM_INLINE_HPP_GUARD\n");
        hpp.append("namespace native_jvm::inlines {\n");
        if (obfuscation.isVerificationEnable() && obfuscation.isUseInternalVerificationInterface()) {
            hpp.append("extern bool licenced;\n");
        }
        for (AbstractInlineMethodBuilder abstractInlineMethodBuilder : this.inlinesInjector) {
            String method = abstractInlineMethodBuilder.buildHpp();
            if (method != null) hpp.append(method);
        }
        hpp.append("}\n");
        hpp.append("#endif\n");
        return hpp.toString();
    }
}
