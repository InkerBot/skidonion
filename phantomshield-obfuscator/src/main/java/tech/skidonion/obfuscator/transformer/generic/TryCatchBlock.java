package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TryCatchBlock extends CodeBlock {
    private TryCatchBlock parent;
    private final List<TryCatchBlock> subTryCatches = new ArrayList<>();
    private LinkedList<CodeBlock> codes = new LinkedList<>();
    private ArrayList<CodeBlock> clone;
    private final CodeBlock endBlock;
    private final int startIndex;
    private final int endIndex;
    private int cloneIndex = 0;
    private final StackCodeBlockMap stackCodeBlockMap = new StackCodeBlockMap();


    public TryCatchBlock(LabelNode startLabel, CodeBlock endBlock, int startIndex, int endIndex) {
        super(startLabel);
        this.endBlock = Objects.requireNonNull(endBlock);
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public InsnList getInstructions() {
        InsnList insns = new InsnList();
        for (CodeBlock code : codes) {
            insns.add(code.getInstructions());
        }
        if (!isSameEndBlockBetweenParent()) insns.add(endBlock.getInstructions());
        return insns;
    }

    private boolean isSameEndBlockBetweenParent() {
        if (parent != null) {
            if (parent.endBlock == endBlock ||
                    parent.endBlock.getLabel() == endBlock.getLabel())
                return true;
            return parent.isSameEndBlockBetweenParent();
        }
        return false;
    }

    @Override
    public Frame<BasicValue> getFrame(int i) {
        return this.codes.getFirst().getFrame(i);
    }

    @Override
    public Frame<BasicValue>[] getFrames() {
        return this.codes.getFirst().getFrames();
    }

    /**
     * update the clone array list to improve performance to get random code block
     */
    public void refreshClonedList() {
        this.clone = new ArrayList<>(this.codes);
        this.cloneIndex = 0;
        for (TryCatchBlock subTryCatch : this.subTryCatches) {
            subTryCatch.refreshClonedList();
        }
    }

    public CodeBlock nextCodeBlock() {
        return this.clone.get(cloneIndex++);
    }

    public ArrayList<CodeBlock> getClonedList() {
        return clone;
    }

    @Override
    public void setInstructions(InsnList instructions) {
        throw new UnsupportedOperationException("Try Catch Code Block can't set instructions as it's provided by its members.");
    }

    @Override
    public AbstractInsnNode[] getOriginInstructions() {
        throw new UnsupportedOperationException("Try Catch Code Block can't get origin instructions as it's provided by its members.");
    }

    public CodeBlock getParent() {
        return parent;
    }

    public void setParent(TryCatchBlock parent) {
        this.parent = parent;
    }

    public CodeBlock getEndBlock() {
        return endBlock;
    }


    public int getStartIndex() {
        return startIndex;
    }


    public int getEndIndex() {
        return endIndex;
    }

    public LinkedList<CodeBlock> getCodes() {
        return codes;
    }

    public void setCodes(LinkedList<CodeBlock> codes) {
        this.codes = codes;
    }

    public void addBlock(CodeBlock block) {
        this.codes.add(block);
    }

    public List<TryCatchBlock> getSubTryCatches() {
        return subTryCatches;
    }

    public void addTryCatchBlock(TryCatchBlock tryCatchBlock) {
        this.subTryCatches.add(tryCatchBlock);
    }

    public StackCodeBlockMap getStackCodeBlockMap() {
        return stackCodeBlockMap;
    }

}
