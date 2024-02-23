package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Label;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.impl.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.impl.generic.TryCatchBlock;

import java.util.*;

public class ControlFlowObfuscation extends Transformer {
    public ControlFlowObfuscation(String name) {
        super(name);
    }

    @Override
    public void transform() throws Exception {
        getFilteredClasses().forEach(cw -> cw.getMethods().stream().filter(this::match).forEach(wrapper -> {
            MethodNode method = wrapper.getMethodNode();

            // analyze all code blocks first
            final Map<LabelNode, CodeBlock> blocksMap = resolveSimpleCodeBlocks(method);

            List<TryCatchBlock> tryCatchList = new LinkedList<>();
            List<CodeBlock> resolvedBlocks = new ArrayList<>(blocksMap.values());
            List<CodeBlock> resultBlocks = new LinkedList<>(blocksMap.values());
            // then resolve try catches
            for (TryCatchBlockNode node : method.tryCatchBlocks) {
                buildTryCatchTree(tryCatchList, resolvedBlocks, resultBlocks, blocksMap, node);
            }

            ResolvedBlocks resolve = new ResolvedBlocks(resolvedBlocks);
            InsnList generated = resolve.toInsnList();
            System.out.println(method.instructions.size() + "|" + generated.size());
            method.instructions = generated;

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

    /**
     * @return is processed result successful
     */
    private void buildTryCatchTree(
            List<TryCatchBlock> tryCatchList,
            List<CodeBlock> resolvedBlocks,
            List<CodeBlock> resultBlocks,
            Map<LabelNode, CodeBlock> blocksMap,
            TryCatchBlockNode node) {
        TryCatchBlock sub = null;
        boolean shouldProcess = true;
        LabelNode startLabel = node.start;
        LabelNode endLabel = node.end;
        int startIndex = blocksMap.get(startLabel).getIndex();
        int endIndex = blocksMap.get(endLabel).getIndex();
        ListIterator<TryCatchBlock> iterator = tryCatchList.listIterator();
        while (iterator.hasNext()) {
            TryCatchBlock it = iterator.next();
            shouldProcess = false;
            if (it.getStartIndex() <= startIndex) {
                if (it.getStartIndex() == startIndex && it.getEndIndex() == endIndex) {
                    return; // same
                } else if (it.getEndIndex() >= endIndex) {
                    buildTryCatchTree(it.getSubTryCatches(), resolvedBlocks, it.getCodes(), blocksMap, node);
                    return; // the new one is parent of it
                } else if (it.getEndIndex() <= startIndex) {
                    continue; // not inclusive
                } else {
                    throw new RuntimeException("impossible try catch length!!??"); // intersection??
                }
            } else {
                if (it.getEndIndex() <= endIndex) {
                    sub = it;
                    shouldProcess = true;
                    break;
                } else if (it.getStartIndex() >= endIndex) {
                    continue; // not inclusive
                } else {
                    throw new RuntimeException("impossible try catch length??!!"); // intersection??
                }
            }
        }
        if (shouldProcess || !iterator.hasNext()) {
            TryCatchBlock tryCatchBlock = new TryCatchBlock(startLabel, blocksMap.get(endLabel), startIndex, endIndex);
            tryCatchBlock.setPrevious(blocksMap.get(startLabel).getPrevious());
            tryCatchBlock.setNext(tryCatchBlock.getEndBlock());
            for (int i = startIndex; i < endIndex; i++) {
                tryCatchBlock.addBlock(resolvedBlocks.get(i));
            }
            if (sub != null) {
                tryCatchBlock.addTryCatchBlock(sub);
                sub.setParent(tryCatchBlock);
                iterator.remove();
                iterator.add(tryCatchBlock);
            }
            ListIterator<CodeBlock> codeIterator = resultBlocks.listIterator();
            while (codeIterator.hasNext()) {
                CodeBlock it = codeIterator.next();
                int index = it.getIndex();
                if (index >= startIndex && index < endIndex) {
                    codeIterator.remove();
                } else if (index == endIndex) {
                    codeIterator.remove();
                    break;
                } else if (index > endIndex) {
                    break;
                }
            }
            codeIterator.add(tryCatchBlock);
            tryCatchList.add(tryCatchBlock);
        }
    }

    private Map<LabelNode, CodeBlock> resolveSimpleCodeBlocks(MethodNode node) {
        final Map<LabelNode, CodeBlock> blocksMap = new LinkedHashMap<>();
        LabelNode start = null;
        CodeBlock previousBlock = null;
        CodeBlock block = null;
        InsnList insns = null;
        int index = 0;
        for (AbstractInsnNode insn : node.instructions) {
            if (insn instanceof LabelNode) {
                if (block != null) {
                    block.setPrevious(previousBlock);
                    block.setInstructions(insns);
                    block.setIndex(index);
                    blocksMap.put(start, block);
                }

                start = ((LabelNode) insn);
                previousBlock = block;
                insns = new InsnList();
                block = new CodeBlock(start);

                if (previousBlock != null) previousBlock.setNext(block);

                index++;
            } else {
                if (start == null) {
                    start = new LabelNode(new Label());
                    insns = new InsnList();
                    block = new CodeBlock(start);
                    insns.add(start);
                }
            }
            insns.add(insn);
        }
        if (block != null) {
            block.setPrevious(previousBlock);
            block.setInstructions(insns);
            block.setIndex(index);
            blocksMap.put(start, block);
        }
        return blocksMap;
    }
}
