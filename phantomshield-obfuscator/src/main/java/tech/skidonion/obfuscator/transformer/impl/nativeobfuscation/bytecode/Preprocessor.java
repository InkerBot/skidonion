package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.value.impls.ModeValue;

public interface Preprocessor {

    void process(ClassNode classNode, MethodNode methodNode, ModeValue mode);
}
