package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import org.objectweb.asm.Type;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
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


    public void buildHeader() {
        // cpp
        cpp.append("#include \"native_jvm.hpp\"\n");
        cpp.append("#include \"native_jvm_inline.hpp\"\n");
        cpp.append("#include <unordered_map>\n");
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


        // hpp
        hpp.append("#include \"native_jvm.hpp\"\n");
        hpp.append("#include <unordered_map>\n");
        hpp.append("#ifndef NATIVE_JVM_INLINE_HPP_GUARD\n");
        hpp.append("#define NATIVE_JVM_INLINE_HPP_GUARD\n");
        hpp.append("namespace native_jvm::inlines {\n");
    }

    public void buildInlineFields() {
        obfuscation.inlineFields.forEach(((key, pair) -> {
            String cppName = pair.getFirst();
            FieldWrapper fw = pair.getSecond();
            String ctype = MethodProcessor.CPP_TYPES[Type.getType(fw.getDescription()).getSort()];
            if (fw.getAccess().isStatic()) {
                cpp.append(ctype).append(" ").append(cppName).append(";\n");
                hpp.append("extern ").append(ctype).append(" ").append(cppName).append(";\n");
            } else {
                cpp.append("std::unordered_map<uintptr_t, ")
                        .append(ctype)
                        .append("> ")
                        .append(cppName)
                        .append(";\n");
                hpp.append("extern ").append("std::unordered_map<uintptr_t, ")
                        .append(ctype)
                        .append("> ")
                        .append(cppName)
                        .append(";\n");
                //std::unordered_map<uintptr_t, int> map;
            }
        }));
    }

    public void buildVerificationField() {
        if (obfuscation.isVerificationEnable()) {
            if (obfuscation.isUseInternalVerificationInterface()) {
                cpp.append("bool licenced = 0;\n");
                hpp.append("extern bool licenced;\n");
            }
            cpp.append("jbyteArray nonce;\n");
            cpp.append("jobject crypto;\n");
            cpp.append("jobject verify_token;\n");
            cpp.append("jbyteArray key;\n");
            cpp.append("jobject username;\n");
            cpp.append("jlong user_id;\n");
            cpp.append("jbyteArray magic_key;\n");

            hpp.append("extern jbyteArray nonce;\n");
            hpp.append("extern jobject crypto;\n");
            hpp.append("extern jobject verify_token;\n");
            hpp.append("extern jbyteArray key;\n");
            hpp.append("extern jobject username;\n");
            hpp.append("extern jlong user_id;\n");
            hpp.append("extern jbyteArray magic_key;\n");
        }

    }

    public void buildInjectInlines() {
        for (AbstractInlineMethodBuilder abstractInlineMethodBuilder : this.inlinesInjector) {
            String _cpp = abstractInlineMethodBuilder.buildCpp();
            if (_cpp != null) cpp.append(_cpp);
            String _hpp = abstractInlineMethodBuilder.buildHpp();
            if (_hpp != null) hpp.append(_hpp);
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
}
