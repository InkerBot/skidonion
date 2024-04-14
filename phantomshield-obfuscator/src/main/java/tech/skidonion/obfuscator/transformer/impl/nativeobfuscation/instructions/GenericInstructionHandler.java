package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.CatchesBlock;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public abstract class GenericInstructionHandler<T extends AbstractInsnNode> implements InstructionTypeHandler<T> {

    protected Map<String, String> props;
    protected String instructionName;
    protected String trimmedTryCatchBlock;

    protected String originTryCatchBlock;

    @Override
    public void accept(MethodContext context, T node) {
        props = new HashMap<>();
        instructionName = MethodProcessor.INSTRUCTIONS.getOrDefault(node.getOpcode(), "NOTFOUND");
        props.put("line", String.valueOf(context.line));
        List<TryCatchBlockNode> tryCatchBlockNodeList = new ArrayList<>();
        for (TryCatchBlockNode tryCatchBlock : context.method.getMethodNode().tryCatchBlocks) {
            if (!context.tryCatches.contains(tryCatchBlock)) {
                continue;
            }
            if (tryCatchBlockNodeList.stream().noneMatch(tryCatchBlockNode ->
                    Objects.equals(tryCatchBlockNode.type, tryCatchBlock.type))) {
                tryCatchBlockNodeList.add(tryCatchBlock);
            }
        }
        StringBuilder tryCatch = new StringBuilder("\n");
        tryCatch.append("    ");
        if (!tryCatchBlockNodeList.isEmpty()) {
            String tryCatchLabelName = context.catches.computeIfAbsent(new CatchesBlock(tryCatchBlockNodeList.stream().map(item ->
                            new CatchesBlock.CatchBlock(item.type, item.handler)).collect(Collectors.toList())),
                    key -> String.format("L_CATCH_%d", context.catches.size()));
            tryCatch.append(context.getSnippets().getSnippet("TRYCATCH_START"));
            tryCatch.append(" goto ").append(tryCatchLabelName).append("; }");
        } else {
            tryCatch.append(context.getSnippets().getSnippet("TRYCATCH_EMPTY", StringUtils.createStringMap(
                    "rettype", MethodProcessor.CPP_TYPES[context.ret.getSort()]
            )));
        }
        String tryCatchString = tryCatch.toString();
        originTryCatchBlock = tryCatchString.trim().replace('\n', ' ');
        props.put("origintrycatchhandler", tryCatchString);
        if (!context.manualTryCatch) {
            props.put("trycatchhandler", tryCatchString);
            trimmedTryCatchBlock = originTryCatchBlock;
        } else {
            props.put("trycatchhandler", "");
            trimmedTryCatchBlock = "";
        }

        props.put("rettype", MethodProcessor.CPP_TYPES[context.ret.getSort()]);

        for (int i = -5; i <= 5; i++) {
            props.put("stackindex" + (i >= 0 ? i : "m" + (-i)), String.valueOf(context.stackPointer + i));
        }

        context.output.append("    ");
        process(context, node);

        if (instructionName != null) {
            context.output.append(context.obfuscator.getSnippets().getSnippet(instructionName, props));
        }
        context.output.append("\n");
    }

    protected abstract void process(MethodContext context, T node);
}
