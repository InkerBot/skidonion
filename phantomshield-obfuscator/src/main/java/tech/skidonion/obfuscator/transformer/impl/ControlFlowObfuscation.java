package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.TryCatchBlock;
import tech.skidonion.obfuscator.transformer.generic.resolver.CodeBlockResolver;

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
            ResolvedBlocks resolve = CodeBlockResolver.resolve(method);


            // TODO shuffle has some bugs
//            // shuffle labels orders
//            InsnList shuffled = new InsnList();
//
//            // goto the entry point
//            shuffled.add(new LabelNode(new Label()));
//            shuffled.add(new JumpInsnNode(GOTO, resolve.getResolvedBlocks().getFirst().getLabel()));
//            shuffle(resolve);
//            shuffled.add(resolve.toInsnList());
//
//            // add a default return value or will loop while compute max stacks/locals
//            Type returnType = Type.getReturnType(method.desc);
//            int opcode = ASMUtils.getReturnOpcode(returnType);
//            shuffled.add(new LabelNode(new Label()));
//            if (opcode != Opcodes.RETURN) shuffled.add(ASMUtils.getDefaultValue(returnType));
//            shuffled.add(new InsnNode(opcode));
//
//            method.instructions = shuffled;

//            Frame<SourceValue>[] frames;
//            try {
//                frames = new Analyzer<>(new SourceInterpreter()).analyze(node.name, node);
//            } catch (AnalyzerException e) {
//                throw new RuntimeException(e);
//            }


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
            CodeBlock next = resolvedBlock.getNext();
            if (next != null) {
                next.getInstructions().add(new JumpInsnNode(Opcodes.GOTO, next.getLabel()));
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
                code.getInstructions().add(new JumpInsnNode(Opcodes.GOTO, next != null ? next.getLabel() : endBlock.getLabel()));
            }
            shuffled.add(codes.getFirst());
            codes.remove();
            ArrayList<CodeBlock> clone = new ArrayList<>(codes);
            Collections.shuffle(clone);
            shuffled.addAll(clone);
            codes = shuffled;
        }

        CodeBlock next = endBlock.getNext();
        if (next != null) {
            next.getInstructions().add(new JumpInsnNode(Opcodes.GOTO, next.getLabel()));
        }
    }

}
