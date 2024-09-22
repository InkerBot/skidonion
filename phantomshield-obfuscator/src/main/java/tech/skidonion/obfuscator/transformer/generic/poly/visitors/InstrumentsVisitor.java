package tech.skidonion.obfuscator.transformer.generic.poly.visitors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.generic.poly.model.Context;
import tech.skidonion.obfuscator.transformer.generic.poly.transforms.*;

public class InstrumentsVisitor implements Visitor<InsnList>, Opcodes {
    private final int encodedVariable;
    private final int decodedVariable;
    private final int indexVariable;
    private final LabelNode loopLabel;
    private final LabelNode outerLabel;

    public InstrumentsVisitor(int encodedVariable, int decodedVariable, int maxLocals) {
        this.encodedVariable = encodedVariable;
        this.decodedVariable = decodedVariable;
        this.indexVariable = maxLocals++;
        this.loopLabel = new LabelNode();
        this.outerLabel = new LabelNode();
    }


    @Override
    public InsnList initialise(Context ctx) {
        InsnList instruments = new InsnList();

        instruments.add(new LabelNode());
        instruments.add(new InsnNode(ICONST_0));
        instruments.add(new VarInsnNode(ISTORE, indexVariable));
        // int i = 0;

        instruments.add(loopLabel);
        instruments.add(new VarInsnNode(ILOAD, indexVariable));
        instruments.add(new VarInsnNode(ALOAD, encodedVariable));
        instruments.add(new InsnNode(ARRAYLENGTH));
        instruments.add(new JumpInsnNode(IF_ICMPGE, outerLabel));
        // i >= encoded.length
        instruments.add(new LabelNode());
        instruments.add(new VarInsnNode(ALOAD, encodedVariable));
        instruments.add(new VarInsnNode(ILOAD, indexVariable));
        instruments.add(new InsnNode(IALOAD));
        // encoded[i] reference
        return instruments;
    }

    @Override
    public void finalise(InsnList in) {
        in.add(new VarInsnNode(ALOAD, decodedVariable));
        in.add(new InsnNode(SWAP));
        in.add(new VarInsnNode(ALOAD, indexVariable));
        in.add(new InsnNode(SWAP));
        in.add(new IntInsnNode(SIPUSH, 255));
        in.add(new InsnNode(IAND));
        in.add(new InsnNode(I2B));
        in.add(new InsnNode(BASTORE));
        // decoded[i] = temp & 0xff

        in.add(new IincInsnNode(indexVariable, 1));
        // i++;

        in.add(new JumpInsnNode(GOTO, loopLabel));
        in.add(outerLabel);
    }

    @Override
    public void visit(Add a, InsnList in) {
        in.add(new LdcInsnNode(a.getValue()));
        in.add(new InsnNode(IADD));
    }

    @Override
    public void visit(Not n, InsnList in) {
        in.add(new InsnNode(ICONST_M1));
        in.add(new InsnNode(IXOR));
    }

    @Override
    public void visit(RotateLeft rl, InsnList in) {
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, rl.lhs()));
        in.add(new InsnNode(IUSHR));
        in.add(new InsnNode(SWAP));
        in.add(new IntInsnNode(BIPUSH, rl.rhs()));
        in.add(new InsnNode(ISHL));
        in.add(new InsnNode(IOR));
    }

    @Override
    public void visit(RotateRight rr, InsnList in) {
        in.add(new InsnNode(DUP));
        in.add(new IntInsnNode(BIPUSH, rr.lhs()));
        in.add(new InsnNode(ISHL));
        in.add(new InsnNode(SWAP));
        in.add(new IntInsnNode(BIPUSH, rr.rhs()));
        in.add(new InsnNode(IUSHR));
        in.add(new InsnNode(IOR));
    }

    @Override
    public void visit(Subtract s, InsnList in) {
        in.add(new LdcInsnNode(s.getValue()));
        in.add(new InsnNode(ISUB));
    }

    @Override
    public void visit(Xor x, InsnList in) {
        in.add(new LdcInsnNode(x.getValue()));
        in.add(new InsnNode(IXOR));
    }
}
