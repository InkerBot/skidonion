package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.tree.IincInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public class IincHandler extends GenericInstructionHandler<IincInsnNode> {

    @Override
    protected void process(MethodContext context, IincInsnNode node) {
        props.put("incr", String.valueOf(node.incr));
        props.put("var", String.valueOf(node.var));
    }

    @Override
    public String insnToString(MethodContext context, IincInsnNode node) {
        return String.format("IINC %d %d", node.var, node.incr);
    }

    @Override
    public int getNewStackPointer(IincInsnNode node, int currentStackPointer) {
        return currentStackPointer;
    }
}
