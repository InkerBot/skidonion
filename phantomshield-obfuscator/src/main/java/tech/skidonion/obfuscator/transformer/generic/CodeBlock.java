package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

public class CodeBlock {
    private CodeBlock previous;
    private CodeBlock next;
    private final LabelNode label;
    private InsnList instructions;
    private int index;

    public CodeBlock(LabelNode startLabel) {
        this.label = startLabel;
    }

    public LabelNode getLabel() {
        return label;
    }

    public InsnList getInstructions() {
        return instructions == null ? (instructions = new InsnList()) : instructions;
    }

    public void setInstructions(InsnList instructions) {
        this.instructions = instructions;
    }

    public CodeBlock getPrevious() {
        return previous;
    }

    public void setPrevious(CodeBlock previous) {
        this.previous = previous;
    }

    public CodeBlock getNext() {
        return next;
    }

    public void setNext(CodeBlock next) {
        this.next = next;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
