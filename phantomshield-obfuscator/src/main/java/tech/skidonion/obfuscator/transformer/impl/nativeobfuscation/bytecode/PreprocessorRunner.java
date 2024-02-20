package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;

import java.util.ArrayList;
import java.util.List;

public class PreprocessorRunner {

    private final static List<Preprocessor> PREPROCESSORS = new ArrayList<>();

    static {
        PREPROCESSORS.add(new IndyPreprocessor());
        PREPROCESSORS.add(new LdcPreprocessor());
    }

    public static void preprocess(NativeObfuscation obfuscation, ClassNode classNode, MethodNode methodNode) {
        for (Preprocessor preprocessor : PREPROCESSORS) {
            preprocessor.process(obfuscation,classNode, methodNode);
        }
    }
}
