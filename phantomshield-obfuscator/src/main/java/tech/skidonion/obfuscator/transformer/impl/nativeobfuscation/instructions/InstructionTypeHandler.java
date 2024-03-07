package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.tree.AbstractInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public interface InstructionTypeHandler<T extends AbstractInsnNode> {
    void accept(MethodContext context, T node);

    String insnToString(MethodContext context, T node);

    int getNewStackPointer(T node, int currentStackPointer);
}
