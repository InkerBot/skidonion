package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;

public interface Preprocessor {
    void process(NativeObfuscation obfuscation, ClassNode classNode, MethodNode methodNode);
}
