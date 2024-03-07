package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.tree.LineNumberNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;

public class LineNumberHandler implements InstructionTypeHandler<LineNumberNode> {
    @Override
    public void accept(MethodContext context, LineNumberNode node) {
        context.line = node.line;
    }

    @Override
    public String insnToString(MethodContext context, LineNumberNode node) {
        return String.format("Line %d", node.line);
    }

    @Override
    public int getNewStackPointer(LineNumberNode node, int currentStackPointer) {
        return currentStackPointer;
    }

}
