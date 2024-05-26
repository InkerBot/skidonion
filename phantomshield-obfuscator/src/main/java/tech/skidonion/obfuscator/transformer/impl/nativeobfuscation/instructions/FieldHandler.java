package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedFieldInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification.BufferContext;
import tech.skidonion.obfuscator.utils.ASMUtils;

public class FieldHandler extends GenericInstructionHandler<FieldInsnNode> {

    @Override
    protected void process(MethodContext context, FieldInsnNode node) {
        boolean isStatic = node.getOpcode() == Opcodes.GETSTATIC || node.getOpcode() == Opcodes.PUTSTATIC;
        CachedFieldInfo info = new CachedFieldInfo(node.owner, node.name, node.desc, isStatic);

        instructionName += "_" + Type.getType(node.desc).getSort();
        if (isStatic) {
            props.put("class_ptr", context.getCachedClasses().getPointer(node.owner));
        }

        int classId = context.getCachedClasses().getId(node.owner);

        context.output.append(MethodProcessor.getClassCacher(context, classId, node.owner, trimmedTryCatchBlock));

        int fieldId = context.getCachedFields().getId(info);
        props.put("fieldid", context.getCachedFields().getPointer(info));

        if (isStatic) {
            if (context.manualTryCatch) {
                context.output.append("_CacheStaticField(env, ").append(classId).append(", ").append(fieldId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(");");
            } else {
                context.output.append("if(_CacheStaticField(env, ").append(classId).append(", ").append(fieldId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(")) ").append(trimmedTryCatchBlock);
            }
        } else {
            if (context.manualTryCatch) {
                context.output.append("_CacheField(env, ").append(classId).append(", ").append(fieldId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(");");
            } else {
                context.output.append("if(_CacheField(env, ").append(classId).append(", ").append(fieldId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(")) ").append(trimmedTryCatchBlock);
            }
        }
    }

    @Override
    public String insnToString(MethodContext context, FieldInsnNode node) {
        return String.format("%s %s.%s %s", ASMUtils.getOpcodeString(node.getOpcode()), node.owner, node.name, node.desc);
    }

    @Override
    public int getNewStackPointer(FieldInsnNode node, int currentStackPointer) {
        if (node.getOpcode() == Opcodes.GETFIELD || node.getOpcode() == Opcodes.PUTFIELD) {
            currentStackPointer -= 1;
        }
        if (node.getOpcode() == Opcodes.GETSTATIC || node.getOpcode() == Opcodes.GETFIELD) {
            currentStackPointer += Type.getType(node.desc).getSize();
        }
        if (node.getOpcode() == Opcodes.PUTSTATIC || node.getOpcode() == Opcodes.PUTFIELD) {
            currentStackPointer -= Type.getType(node.desc).getSize();
        }
        return currentStackPointer;
    }
}
