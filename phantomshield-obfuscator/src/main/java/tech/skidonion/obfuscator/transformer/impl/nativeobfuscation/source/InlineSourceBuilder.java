package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.crypto.ChaCha20;
import tech.skidonion.obfuscator.inline.Wrapper;
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
                Optional<String> opt = Wrapper.getCloudConstant(467287013, 0);
                if (obfuscation.isVerificationEnable() && opt.isPresent() && (Integer.parseInt(opt.get()) ^ 173359771) == 2082061244) {
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

        if (obfuscation.isVerificationEnable()) {
            if (obfuscation.isUseInternalVerificationInterface()) {
                cpp.append("bool licenced = 0;\n");
            }
            cpp.append("jbyteArray nonce;\n");
            cpp.append("jobject crypto;\n");
            cpp.append("jobject verify_token;\n");
            cpp.append("jbyteArray key;\n");
            cpp.append("jobject username;\n");
            cpp.append("jlong user_id;\n");
            cpp.append("jbyteArray magic_key;\n");
//            private static byte[] NONCE;
//            private static ChaCha20 CRYPTO;
//            private static String VERIFY_TOKEN;
//            private static byte[] KEY;
//            private static String USERNAME;
//            private static long USER_ID;
//            private static byte[] MAGIC_KEY;
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
        if (obfuscation.isVerificationEnable()) {
            if (obfuscation.isUseInternalVerificationInterface()) {
                hpp.append("extern bool licenced;\n");
            }
            cpp.append("extern jbyteArray nonce;\n");
            cpp.append("extern jobject crypto;\n");
            cpp.append("extern jobject verify_token;\n");
            cpp.append("extern jbyteArray key;\n");
            cpp.append("extern jobject username;\n");
            cpp.append("extern jlong user_id;\n");
            cpp.append("extern jbyteArray magic_key;\n");
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
