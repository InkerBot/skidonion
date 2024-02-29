package tech.skidonion.obfuscator.transformer.generic.resolver;

import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;
import tech.skidonion.obfuscator.asm.SimpleInterpreter;
import tech.skidonion.obfuscator.transformer.generic.CodeBlock;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.TryCatchBlock;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CodeBlockResolver implements Opcodes {
    public static ResolvedBlocks resolve(MethodNode method) {
        final List<Type> localTypes = new ArrayList<>(Arrays.asList(new Type[method.maxLocals]));
        // analyze all code blocks first
        final Map<LabelNode, CodeBlock> blocksMap = resolveSimpleCodeBlocks(method, localTypes);

        List<TryCatchBlock> tryCatchList = new LinkedList<>();
        List<CodeBlock> resolvedBlocks = new ArrayList<>(blocksMap.values());
        List<CodeBlock> resultBlocks = new LinkedList<>(blocksMap.values());
        // then resolve try catches
        for (TryCatchBlockNode node : method.tryCatchBlocks) {
            buildTryCatchTree(tryCatchList, resolvedBlocks, resultBlocks, blocksMap, node);
        }

        return new ResolvedBlocks(tryCatchList, resultBlocks, localTypes);
    }

    private static void buildTryCatchTree(
            List<TryCatchBlock> tryCatchList,
            List<CodeBlock> resolvedBlocks,
            List<CodeBlock> resultBlocks,
            Map<LabelNode, CodeBlock> blocksMap,
            TryCatchBlockNode node) {
        TryCatchBlock sub = null;
        LabelNode startLabel = node.start;
        LabelNode endLabel = node.end;
        int startIndex = blocksMap.get(startLabel).getIndex();
        int endIndex = blocksMap.get(endLabel).getIndex();
        ListIterator<TryCatchBlock> iterator = tryCatchList.listIterator();
        while (iterator.hasNext()) {
            TryCatchBlock it = iterator.next();
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
                    break;
                } else if (it.getStartIndex() >= endIndex) {
                    continue; // not inclusive
                } else {
                    throw new RuntimeException("impossible try catch length??!!"); // intersection??
                }
            }
        }
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

    @SuppressWarnings("unchecked")
    private static Map<LabelNode, CodeBlock> resolveSimpleCodeBlocks(MethodNode node, List<Type> localTypes) {
        AtomicInteger maxLocals = new AtomicInteger(node.maxLocals);
        Frame<BasicValue>[] frames;

        // TODO
        try {
            frames = new Analyzer<>(new SimpleInterpreter()).analyze(node.name, node);
        } catch (AnalyzerException e) {
            throw new RuntimeException(e);
        }
        int insnIndex = 0;
        int lastGroupInsnIndex = 0;
        final Map<Integer, Integer> variablesMap = new HashMap<>();
        final Map<LabelNode, CodeBlock> blocksMap = new LinkedHashMap<>();
        LabelNode start = null;
        CodeBlock previousBlock = null;
        CodeBlock block = null;
        InsnList insns = new InsnList();
        int index = -1;
        for (AbstractInsnNode insn : node.instructions) {
            if (insn instanceof LabelNode) {
                if (block != null) {
                    block.setPrevious(previousBlock);
                    block.setInstructions(insns);
                    block.setIndex(index);

                    int length = insnIndex - lastGroupInsnIndex;
                    Frame<?>[] subFrames = new Frame[length];
                    System.arraycopy(frames, lastGroupInsnIndex, subFrames, 0, length);
                    block.setFrames((Frame<BasicValue>[]) subFrames);
                    lastGroupInsnIndex = insnIndex;

                    blocksMap.put(start, block);
                }

                start = ((LabelNode) insn);
                previousBlock = block;
                insns = new InsnList();
                block = new CodeBlock(start);

                if (previousBlock != null) previousBlock.setNext(block);


                index++;
            } else if (insn instanceof VarInsnNode) {

                // TODO
                VarInsnNode varInsnNode = (VarInsnNode) insn;
                switch (insn.getOpcode()) {
                    case ISTORE:
                    case ILOAD:
                        processLocals(varInsnNode, maxLocals, localTypes, variablesMap, Type.INT, Type.INT_TYPE);
                        break;
                    case FSTORE:
                    case FLOAD:
                        processLocals(varInsnNode, maxLocals, localTypes, variablesMap, Type.FLOAT, Type.FLOAT_TYPE);
                        break;
                    case LLOAD:
                    case LSTORE:
                        processLocals(varInsnNode, maxLocals, localTypes, variablesMap, Type.LONG, Type.LONG_TYPE);
                        break;
                    case DLOAD:
                    case DSTORE:
                        processLocals(varInsnNode, maxLocals, localTypes, variablesMap, Type.DOUBLE, Type.DOUBLE_TYPE);
                        break;
                    case ALOAD:
                    case ASTORE:
                        processLocals(varInsnNode, maxLocals, localTypes, variablesMap, Type.OBJECT, Type.getType("Ljava/lang/Object;"));
                        break;
                    case RET:
                        throw new RuntimeException("can't resolve RET opcode");
                }
            } else {
                if (start == null) {
                    start = new LabelNode(new Label());
                    insns = new InsnList();
                    block = new CodeBlock(start);
                    insns.add(start);
                    index++;
                }
            }
            insns.add(insn);
            insnIndex++;
        }
        if (block != null) {
            block.setPrevious(previousBlock);
            block.setInstructions(insns);
            block.setIndex(index);

            int length = insnIndex - lastGroupInsnIndex;
            Frame<?>[] subFrames = new Frame[length];
            System.arraycopy(frames, lastGroupInsnIndex, subFrames, 0, length);
            block.setFrames((Frame<BasicValue>[]) subFrames);

            blocksMap.put(start, block);
        }
        node.maxLocals = maxLocals.get();
        return blocksMap;
    }


    /**
     * process local variables which used by a same index
     */
    private static void processLocals(VarInsnNode varInsnNode, AtomicInteger maxLocals, List<Type> localTypes, Map<Integer, Integer> variablesMap, int sort, Type localType) {
        // TODO
        Type type = localTypes.get(varInsnNode.var);
        if (type == null) {
            localTypes.set(varInsnNode.var, localType);
        } else if (type.getSort() != sort && (!variablesMap.containsKey(varInsnNode.var) || localTypes.get(variablesMap.get(varInsnNode.var)) != localType)) {
            int size = (sort == Type.LONG || sort == Type.DOUBLE) ? 2 : 1;
            int index = maxLocals.getAndAdd(size);
            localTypes.add(localType);
            variablesMap.put(varInsnNode.var, index);
        }
        if (variablesMap.containsKey(varInsnNode.var)) {
            varInsnNode.var = variablesMap.get(varInsnNode.var);
        }
    }
}
