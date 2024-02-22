package tech.skidonion.obfuscator.transformer.impl.generic;

import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TryCatchBlock extends CodeBlock {
    private CodeBlock parent;
    private final List<TryCatchBlock> subTryCatches = new ArrayList<>();
    private final List<CodeBlock> codes = new LinkedList<>();
    private CodeBlock endBlock;
    private int startIndex;
    private int endIndex;

    public TryCatchBlock(LabelNode startLabel) {
        super(startLabel);
    }

    public CodeBlock getParent() {
        return parent;
    }

    public void setParent(CodeBlock parent) {
        this.parent = parent;
    }

    public CodeBlock getEndBlock() {
        return endBlock;
    }

    public void setEndBlock(CodeBlock endBlock) {
        this.endBlock = endBlock;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }

    public List<CodeBlock> getCodes() {
        return codes;
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
}
