package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.TryCatchBlock;
import tech.skidonion.obfuscator.transformer.generic.resolver.CodeBlockResolver;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

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
            ResolvedBlocks resolve = CodeBlockResolver.resolve(method);

            // shuffle labels orders
            InsnList shuffled = new InsnList();

            // TODO: compute stack frame height and type
            // goto the entry point
            shuffled.add(new LabelNode(new Label()));
            int i = (ASMUtils.getFlag(method.access, Opcodes.ACC_STATIC) ? 0 : 1) + Type.getArgumentTypes(method.desc).length;
            for (int index = i; index < resolve.getLocals().length; index++) {
                Type type = resolve.getLocals()[index];
                if (type != null) {
                    shuffled.add(ASMUtils.getDefaultValue(type));
                    shuffled.add(new VarInsnNode(ASMUtils.getVarOpcode(type, true), index));
                }
            }
            shuffled.add(new JumpInsnNode(GOTO, resolve.getResolvedBlocks().getFirst().getLabel()));
            shuffle(resolve);
            shuffled.add(resolve.toInsnList());


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
                shuffle((TryCatchBlock) resolvedBlock);
                continue;
            }
            InsnList insns = resolvedBlock.getInstructions();
            CodeBlock next = resolvedBlock.getNext();
            AbstractInsnNode insn = resolvedBlock.getInstructions().getLast();
            if (next != null && insn != null && !ASMUtils.isJumpOrReturnOpcode(insn.getOpcode())) {
                boolean generate = RandomUtils.getRandomBoolean();
                boolean if_equals = RandomUtils.getRandomBoolean();
                LabelNode label1;
                LabelNode label2;
                if ((generate && if_equals) || (!generate && !if_equals)) {
                    label1 = next.getLabel();
                    label2 = resolved.getRandomCodeBlock(null).getLabel();
                } else {
                    label2 = next.getLabel();
                    label1 = resolved.getRandomCodeBlock(null).getLabel();
                }
                insns.add(generate ? ASMUtils.generateFalse() : ASMUtils.generateTrue());
                insns.add(new JumpInsnNode(if_equals ? IFEQ : IFNE, label1));
                insns.add(new JumpInsnNode(Opcodes.GOTO, label2));


//                insns.add(new InsnNode(ICONST_1));
//                insns.add(new JumpInsnNode(IFEQ, resolvedBlock.getLabel()));
//                insns.add(new JumpInsnNode(Opcodes.GOTO, next.getLabel()));
            }
        }
        ArrayList<CodeBlock> clone = new ArrayList<>(resolved.getResolvedBlocks());
        Collections.shuffle(clone);
        resolved.setResolvedBlocks(new LinkedList<>(clone));
    }

    private void shuffle(TryCatchBlock tryCatchBlock) {
        LinkedList<CodeBlock> codes = tryCatchBlock.getCodes();
        CodeBlock endBlock = tryCatchBlock.getEndBlock();

        LinkedList<CodeBlock> shuffled = new LinkedList<>();
        if (codes.size() > 2) {
            for (CodeBlock code : codes) {
                if (code instanceof TryCatchBlock) {
                    shuffle((TryCatchBlock) code);
                    continue;
                }
                CodeBlock next = code.getNext();
                AbstractInsnNode insn = code.getInstructions().getLast();
                if (insn != null && !ASMUtils.isJumpOrReturnOpcode(insn.getOpcode())) {
                    code.getInstructions().add(new JumpInsnNode(Opcodes.GOTO, next != null ? next.getLabel() : endBlock.getLabel()));
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

}
