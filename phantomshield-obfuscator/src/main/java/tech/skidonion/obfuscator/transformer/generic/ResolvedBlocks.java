package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class ResolvedBlocks {
    private final ArrayList<TryCatchBlock> tryCatchBlocks;
    private LinkedList<CodeBlock> resolvedBlocks;
    private final List<Type> locals;
    private ArrayList<CodeBlock> clone;
    private final StackCodeBlockMap stackCodeBlockMap = new StackCodeBlockMap();
    private int cloneIndex;

    public ResolvedBlocks(Collection<TryCatchBlock> tryCatchBlocks, Collection<CodeBlock> resolvedBlocks, List<Type> locals) {
        this.tryCatchBlocks = new ArrayList<>(tryCatchBlocks);
        for (TryCatchBlock tryCatchBlock : this.tryCatchBlocks) {
            tryCatchBlock.refreshClonedList();
        }
        this.resolvedBlocks = new LinkedList<>(resolvedBlocks);
        this.clone = new ArrayList<>(resolvedBlocks);
        this.cloneIndex = 0;
        this.locals = locals;
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
        this.clone = new ArrayList<>(resolvedBlocks);
        this.cloneIndex = 0;
    }

    /**
     * update the clone array list to improve performance to get random code block
     */
    public void refreshClonedList() {
        this.clone = new ArrayList<>(this.resolvedBlocks);
        this.cloneIndex = 0;
    }

    public ArrayList<CodeBlock> getClonedList() {
        return clone;
    }

    public CodeBlock nextCloneBlock() {
        return clone.get(cloneIndex++);
    }

    public LinkedList<CodeBlock> getResolvedBlocks() {
        return resolvedBlocks;
    }

    public ArrayList<TryCatchBlock> getTryCatchBlocks() {
        return tryCatchBlocks;
    }

    public CodeBlock getRandomCodeBlock() {
        return this.clone.get(RandomUtils.getRandomInt(this.clone.size()));
    }

    public List<Type> getLocals() {
        return locals;
    }

    public StackCodeBlockMap getStackCodeBlockMap() {
        return stackCodeBlockMap;
    }
}
