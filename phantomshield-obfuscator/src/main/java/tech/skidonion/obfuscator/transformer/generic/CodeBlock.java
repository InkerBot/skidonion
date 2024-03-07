package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;

public class CodeBlock {
    private Frame<BasicValue>[] frames;
    private AbstractInsnNode[] originInstructions;
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
        AbstractInsnNode node = this.instructions.getFirst();
        if (this.originInstructions == null) {
            this.originInstructions = new AbstractInsnNode[instructions.size()];
            for (int i = 0; i < instructions.size(); i++) {
                this.originInstructions[i] = node;
                node = node.getNext();
            }
        }
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

    public Frame<BasicValue>[] getFrames() {
        return frames;
    }

    public Frame<BasicValue> getFrame(int i) {
        return frames[i];
    }

    public void setFrames(Frame<BasicValue>[] frames) {
        this.frames = frames;
    }

    public AbstractInsnNode[] getOriginInstructions() {
        return originInstructions;
    }
}
