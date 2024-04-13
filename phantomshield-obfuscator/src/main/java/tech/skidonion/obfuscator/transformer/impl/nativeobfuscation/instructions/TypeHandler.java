package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.TypeInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.utils.ASMUtils;

public class TypeHandler extends GenericInstructionHandler<TypeInsnNode> {

    @Override
    protected void process(MethodContext context, TypeInsnNode node) {
        props.put("desc", node.desc);

        int classId = context.getCachedClasses().getId(node.desc);
        context.output.append(MethodProcessor.getClassCacher(context, classId, node.desc, trimmedTryCatchBlock));


        props.put("desc_ptr", context.getCachedClasses().getPointer(node.desc));
    }

    @Override
    public String insnToString(MethodContext context, TypeInsnNode node) {
        return String.format("%s %s", ASMUtils.getOpcodeString(node.getOpcode()), node.desc);
    }

    @Override
    public int getNewStackPointer(TypeInsnNode node, int currentStackPointer) {
        switch (node.getOpcode()) {
            case Opcodes.ANEWARRAY:
            case Opcodes.CHECKCAST:
            case Opcodes.INSTANCEOF:
                return currentStackPointer;
            case Opcodes.NEW:
                return currentStackPointer + 1;
        }
        throw new RuntimeException();
    }
}
