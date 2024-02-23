package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.tree.InsnList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

public class ResolvedBlocks {
    private final ArrayList<TryCatchBlock> tryCatchBlocks;
    private LinkedList<CodeBlock> resolvedBlocks;

    public ResolvedBlocks(Collection<TryCatchBlock> tryCatchBlocks, Collection<CodeBlock> resolvedBlocks) {
        this.tryCatchBlocks = new ArrayList<>(tryCatchBlocks);
        this.resolvedBlocks = new LinkedList<>(resolvedBlocks);
    }

    public InsnList toInsnList() {
        InsnList insns = new InsnList();
        for (CodeBlock block : resolvedBlocks) {
            insns.add(block.getInstructions());
        }
        return insns;
    }

    public void setResolvedBlocks(LinkedList<CodeBlock> resolvedBlocks) {
        this.resolvedBlocks = resolvedBlocks;
    }

    public LinkedList<CodeBlock> getResolvedBlocks() {
        return resolvedBlocks;
    }

    public ArrayList<TryCatchBlock> getTryCatchBlocks() {
        return tryCatchBlocks;
    }
}
