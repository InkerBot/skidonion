package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class TryCatchBlock extends CodeBlock {
    private TryCatchBlock parent;
    private final List<TryCatchBlock> subTryCatches = new ArrayList<>();
    private LinkedList<CodeBlock> codes = new LinkedList<>();
    private final CodeBlock endBlock;
    private final int startIndex;
    private final int endIndex;

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
                    parent.endBlock.getLabel() == endBlock.getLabel() ||
                    parent.endBlock.getLabel().getLabel() == endBlock.getLabel().getLabel())
                return true;
            return parent.isSameEndBlockBetweenParent();
        }
        return false;
    }

    @Override
    public void setInstructions(InsnList instructions) {
        throw new UnsupportedOperationException("Try Catch Code Block can't set Instructions as it's provided by its members.");
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
}
