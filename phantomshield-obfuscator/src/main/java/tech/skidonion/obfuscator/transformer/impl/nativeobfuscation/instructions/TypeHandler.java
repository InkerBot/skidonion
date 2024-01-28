package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.TypeInsnNode;
import tech.skidonion.obfuscator.utils.ASMUtils;

public class TypeHandler extends GenericInstructionHandler<TypeInsnNode> {

    @Override
    protected void process(MethodContext context, TypeInsnNode node) {
        props.put("desc", node.desc);

        int classId = context.getCachedClasses().getId(node.desc);
        context.output.append(String.format("if (!cclasses[%d] || env->IsSameObject(cclasses[%d], NULL)) { cclasses_mtx[%d].lock(); if (!cclasses[%d] || env->IsSameObject(cclasses[%d], NULL)) { if (jclass clazz = %s) { cclasses[%d] = (jclass) env->NewWeakGlobalRef(clazz); env->DeleteLocalRef(clazz); } } cclasses_mtx[%d].unlock(); %s } ",
                classId,
                classId,
                classId,
                classId,
                classId,
                MethodProcessor.getClassGetter(context, node.desc),
                classId,
                classId,
                trimmedTryCatchBlock));

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
