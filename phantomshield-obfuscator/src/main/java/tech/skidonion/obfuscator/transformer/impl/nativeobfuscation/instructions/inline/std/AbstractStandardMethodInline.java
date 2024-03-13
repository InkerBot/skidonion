package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std;

import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public abstract class AbstractStandardMethodInline {
    public abstract void process(String desc, MethodContext context, MethodInsnNode node);

    public abstract String[] methods();
}
