package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source;

import tech.skidonion.obfuscator.cpp.CppCompiler;

public abstract class AbstractInlineMethodBuilder {

    protected final CppCompiler compiler;
    private final String prefixVM;

    public AbstractInlineMethodBuilder(CppCompiler compiler) {
        this.compiler = compiler;
        this.prefixVM = compiler.isAdvancedModuleEnable() ? "VM" : "VIRTUALIZER";
    }


    protected StringBuilder cpp = new StringBuilder();
    protected StringBuilder hpp = new StringBuilder();

    public abstract String[] injectHeader();

    public abstract String buildCpp();

    public abstract String buildHpp();

    protected String vmStart() {
        return prefixVM + "_TIGER_WHITE_START\n";
    }

    protected String vmEnd() {
        return prefixVM + "_TIGER_WHITE_END\n";
    }
}
