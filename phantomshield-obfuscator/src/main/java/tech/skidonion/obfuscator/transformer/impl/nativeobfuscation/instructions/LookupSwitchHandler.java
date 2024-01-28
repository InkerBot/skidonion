package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import tech.skidonion.obfuscator.utils.StringUtils;

public class LookupSwitchHandler extends GenericInstructionHandler<LookupSwitchInsnNode> {

    @Override
    protected void process(MethodContext context, LookupSwitchInsnNode node) {
        StringBuilder output = context.output;

        output.append(getStart(context)).append("\n    ");

        for (int i = 0; i < node.labels.size(); ++i) {
            output.append(String.format("    %s\n    ", getPart(context,
                    node.keys.get(i),
                    node.labels.get(i).getLabel())));
        }
        output.append(String.format("    %s\n    ", getDefault(context, node.dflt.getLabel())));

        instructionName = "LOOKUPSWITCH_END";
    }

    private static String getStart(MethodContext context) {
        return context.getSnippets().getSnippet("LOOKUPSWITCH_START", StringUtils.createMap(
                "stackindexm1", String.valueOf(context.stackPointer - 1)
        ));
    }

    private static String getPart(MethodContext context, int key, Label label) {
        return context.getSnippets().getSnippet("LOOKUPSWITCH_PART", StringUtils.createMap(
                "key", key,
                "label", context.getLabelPool().getName(label)
        ));
    }

    private static String getDefault(MethodContext context, Label label) {
        return context.getSnippets().getSnippet("LOOKUPSWITCH_DEFAULT", StringUtils.createMap(
                "label", context.getLabelPool().getName(label)
        ));
    }

    @Override
    public String insnToString(MethodContext context, LookupSwitchInsnNode node) {
        return "LOOKUPSWITCH";
    }

    @Override
    public int getNewStackPointer(LookupSwitchInsnNode node, int currentStackPointer) {
        return currentStackPointer - 1;
    }
}
