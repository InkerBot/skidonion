package tech.skidonion.obfuscator.transformer.impl.generic;

import org.objectweb.asm.tree.InsnList;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ResolvedBlocks {
    private final List<CodeBlock> resolvedBlocks;

    public ResolvedBlocks(Collection<CodeBlock> resolvedBlocks) {
        this.resolvedBlocks = new LinkedList<>(resolvedBlocks);
    }

    public InsnList toInsnList() {
        InsnList insns = new InsnList();
        for (CodeBlock block : resolvedBlocks) {
            insns.add(block.getInstructions());
        }
        return insns;
    }

    public List<CodeBlock> getResolvedBlocks() {
        return resolvedBlocks;
    }
}
