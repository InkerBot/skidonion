package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.TryCatchBlock;
import tech.skidonion.obfuscator.transformer.generic.resolver.CodeBlockResolver;
import tech.skidonion.obfuscator.utils.ASMUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

public class ControlFlowObfuscation extends Transformer implements Opcodes {
    public ControlFlowObfuscation(String name) {
        super(name);
    }

    @Override
    public void transform() throws Exception {
        getFilteredClasses().forEach(cw -> cw.getMethods().stream().filter(wrapper -> wrapper.getInstructions().size() > 0 && this.match(wrapper)).forEach(wrapper -> {
            MethodNode method = wrapper.getMethodNode();
            // delete the fuck shit
            method.localVariables = null;
            ResolvedBlocks resolved = CodeBlockResolver.resolve(method);

            // add opaque predications
            this.addOpaquePredicate(resolved);

            // shuffle labels orders
            InsnList shuffled = new InsnList();

            // initialization all variables
            shuffled.add(new LabelNode(new Label()));
            int i = (ASMUtils.getFlag(method.access, Opcodes.ACC_STATIC) ? 0 : 1) + Type.getArgumentTypes(method.desc).length;
            for (int index = i; index < resolved.getLocals().length; index++) {
                Type type = resolved.getLocals()[index];
                if (type != null) {
                    shuffled.add(ASMUtils.getDefaultValue(type));
                    shuffled.add(new VarInsnNode(ASMUtils.getVarOpcode(type, true), index));
                }
            }
            // goto the entry point
            shuffled.add(new LabelNode(new Label()));
            shuffled.add(new JumpInsnNode(GOTO, resolved.getResolvedBlocks().getFirst().getLabel()));
            shuffle(resolved);
            shuffled.add(resolved.toInsnList());


            // add a default return value or will loop while compute max stacks/locals
            Type returnType = Type.getReturnType(method.desc);
            int opcode = ASMUtils.getReturnOpcode(returnType);
            shuffled.add(new LabelNode(new Label()));
            if (opcode != Opcodes.RETURN) shuffled.add(ASMUtils.getDefaultValue(returnType));
            shuffled.add(new InsnNode(opcode));

            method.instructions = shuffled;
        }));
    }

    @Override
    public void preprocess() throws Exception {
    }

    @Override
    public String annotation() {
        return null;
    }

    private void shuffle(ResolvedBlocks resolved) {
        for (CodeBlock resolvedBlock : resolved.getResolvedBlocks()) {
            if (resolvedBlock instanceof TryCatchBlock) {
                shuffle(resolved, (TryCatchBlock) resolvedBlock);
                continue;
            }
        }
        ArrayList<CodeBlock> clone = new ArrayList<>(resolved.getResolvedBlocks());
        Collections.shuffle(clone);
        resolved.setResolvedBlocks(new LinkedList<>(clone));
    }

    private void shuffle(ResolvedBlocks resolved, TryCatchBlock tryCatchBlock) {
        LinkedList<CodeBlock> codes = tryCatchBlock.getCodes();
        CodeBlock endBlock = tryCatchBlock.getEndBlock();

        LinkedList<CodeBlock> shuffled = new LinkedList<>();
        if (codes.size() > 2) {
            for (CodeBlock code : codes) {
                if (code instanceof TryCatchBlock) {
                    shuffle(resolved, (TryCatchBlock) code);
                    continue;
                }
            }
            shuffled.add(codes.getFirst());
            codes.remove();
            ArrayList<CodeBlock> clone = new ArrayList<>(codes);
            Collections.shuffle(clone);
            shuffled.addAll(clone);
            tryCatchBlock.setCodes(shuffled);
        }

        CodeBlock next = endBlock.getNext();
        if (next != null) {
            endBlock.getInstructions().add(new JumpInsnNode(Opcodes.GOTO, next.getLabel()));
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved) {
        for (CodeBlock code : resolved.getResolvedBlocks()) {
            if (code instanceof TryCatchBlock) {
                addOpaquePredicate(resolved, (TryCatchBlock) code);
                continue;
            }
            addOpaquePredicate(resolved, code);
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved, TryCatchBlock tryCatchBlock) {
        for (CodeBlock code : tryCatchBlock.getCodes()) {
            if (code instanceof TryCatchBlock) {
                addOpaquePredicate(resolved, (TryCatchBlock) code);
                continue;
            }
            addOpaquePredicate(resolved, code);
        }
    }

    private void addOpaquePredicate(ResolvedBlocks resolved, CodeBlock code) {
        InsnList insns = code.getInstructions();
        CodeBlock next = code.getNext();
        AbstractInsnNode insn = code.getInstructions().getLast();
        if (next != null && insn != null && !ASMUtils.isJumpOrReturnOpcode(insn.getOpcode())) {

            // TODO: the fuck opaque predications
//            boolean generate = RandomUtils.getRandomBoolean();
//            boolean if_equals = RandomUtils.getRandomBoolean();
//            LabelNode label1;
//            LabelNode label2;
//            if ((generate && if_equals) || (!generate && !if_equals)) {
//                label1 = next.getLabel();
//                label2 = resolved.getRandomCodeBlock(null).getLabel();
//            } else {
//                label2 = next.getLabel();
//                label1 = resolved.getRandomCodeBlock(null).getLabel();
//            }
//            insns.add(generate ? ASMUtils.generateFalse() : ASMUtils.generateTrue());
//            insns.add(new JumpInsnNode(if_equals ? IFEQ : IFNE, label1));
//            insns.add(new JumpInsnNode(Opcodes.GOTO, label2));


            insns.add(new InsnNode(ICONST_1));
            insns.add(new JumpInsnNode(IFEQ, code.getLabel()));
            insns.add(new JumpInsnNode(Opcodes.GOTO, next.getLabel()));
        }
    }

}
