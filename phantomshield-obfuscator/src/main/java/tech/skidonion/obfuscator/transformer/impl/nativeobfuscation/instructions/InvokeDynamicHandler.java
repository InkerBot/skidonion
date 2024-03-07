package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public class InvokeDynamicHandler extends GenericInstructionHandler<InvokeDynamicInsnNode> {

    @Override
    protected void process(MethodContext context, InvokeDynamicInsnNode node) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }

    @Override
    public String insnToString(MethodContext context, InvokeDynamicInsnNode node) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }

    @Override
    public int getNewStackPointer(InvokeDynamicInsnNode node, int currentStackPointer) {
        throw new RuntimeException("Indy should be handled at bytecode side");
    }
}
