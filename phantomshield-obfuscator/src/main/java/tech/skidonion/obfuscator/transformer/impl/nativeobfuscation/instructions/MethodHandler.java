package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import tech.skidonion.obfuscator.inline.Inline;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenMethodsPool;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode.PreprocessorUtils;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedMethodInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.InlineHandler;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.InlineRegister;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification.BufferContext;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.IOUtils;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.util.*;
import java.util.stream.Stream;

public class MethodHandler extends GenericInstructionHandler<MethodInsnNode> {

    private final Set<String> inlines = new HashSet<>();
    private final InlineRegister inlineProcessor = new InlineRegister();

    public MethodHandler() {
        inlines.addAll(inlineProcessor.init());
    }

    private static Type simplifyType(Type type) {
        switch (type.getSort()) {
            case Type.OBJECT:
            case Type.ARRAY:
                return Type.getObjectType("java/lang/Object");
            case Type.METHOD:
                throw new RuntimeException();
        }
        return type;
    }

    private static String simplifyDesc(String desc) {
        return Type.getMethodType(simplifyType(Type.getReturnType(desc)), Arrays.stream(Type.getArgumentTypes(desc))
                .map(MethodHandler::simplifyType).toArray(Type[]::new)).getDescriptor();
    }

    @Override
    protected void process(MethodContext context, MethodInsnNode node) {
        if (node.owner.equals(NativeObfuscation.INLINE_DECLARE)) {
            InlineHandler.process(context, node, originTryCatchBlock);
            instructionName = null;
            return;
        }

        String desc = node.owner + "." + node.name + node.desc;
        if (inlines.contains(desc)) {
            inlineProcessor.process(desc, context, node);
            instructionName = null;
            return;
        }

        if (PreprocessorUtils.isLookupLocal(node)) {
            context.output.append("if (lookup == nullptr) { lookup = utils::get_lookup(env, clazz); ")
                    .append(trimmedTryCatchBlock).append(" } cstack").append(context.stackPointer).append(".l = lookup;");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isClassLoaderLocal(node)) {
            context.output.append("cstack").append(context.stackPointer).append(".l = classloader;");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isClassLocal(node)) {
            context.output.append("cstack").append(context.stackPointer).append(".l = clazz;");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isGetCallSite(node)) {
            context.output.append("cstack").append(context.stackPointer).append(".l = ccallsites[").append(context.obfuscator.getCachedCallSitesIndex().get()).append("];");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isGetCallSiteAndIncrement(node)) {
            context.output.append("cstack").append(context.stackPointer).append(".l = ccallsites[").append(context.obfuscator.getCachedCallSitesIndex().getAndIncrement()).append("];");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isCacheCallSite(node)) {
//            ccallsites[0] = env->NewGlobalRef(cstack1.l);
//            env->DeleteLocalRef(cstack1.l);
            context.output.append("ccallsites[").append(context.obfuscator.getCachedCallSitesIndex().get()).append("] = ").append("env->NewGlobalRef(cstack").append(context.stackPointer - 1).append(".l);").append("env->DeleteLocalRef(cstack").append(context.stackPointer - 1).append(".l);");
            instructionName = null;
            return;
        }
        if (PreprocessorUtils.isInvokeReverse(node)) {
            // stack - args, mh
            String methodDesc = simplifyDesc(node.desc);
            Type[] methodArguments = Type.getArgumentTypes(methodDesc);
            methodArguments[methodArguments.length - 1] = Type.getObjectType("java/lang/invoke/MethodHandle");
            methodDesc = Type.getMethodDescriptor(Type.getReturnType(methodDesc), methodArguments);
            String mhDesc = simplifyDesc(Type.getMethodType(Type.getReturnType(node.desc),
                    IOUtils.reverse(IOUtils.reverse(Arrays.stream(Type.getArgumentTypes(node.desc)))
                            .skip(1)).toArray(Type[]::new)).getDescriptor());

            HiddenMethodsPool.HiddenMethod hiddenMethod = context.obfuscator.getHiddenMethodsPool()
                    .getMethod("__", methodDesc, method -> {
                        method.visibleAnnotations = new ArrayList<>();
                        method.visibleAnnotations.add(new AnnotationNode("Ljava/lang/invoke/LambdaForm$Hidden;"));
                        method.visibleAnnotations.add(new AnnotationNode("Ljdk/internal/vm/annotation/Hidden;"));
                        int methodHandleIndex = 0;
                        for (Type argument : Type.getArgumentTypes(mhDesc)) {
                            methodHandleIndex += argument.getSize();
                        }
                        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, methodHandleIndex));
                        int index = 0;
                        for (Type argument : Type.getArgumentTypes(mhDesc)) {
                            method.instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), index));
                            index += argument.getSize();
                        }
                        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                "java/lang/invoke/MethodHandle", "invoke", mhDesc));
                        method.instructions.add(new InsnNode(Type.getReturnType(mhDesc).getOpcode(Opcodes.IRETURN)));
                    });

            node = (MethodInsnNode) node.clone(null);
            node.name = hiddenMethod.getMethodNode().name;
            node.owner = hiddenMethod.getClassNode().name;
            node.desc = hiddenMethod.getMethodNode().desc;
            node.setOpcode(Opcodes.INVOKESTATIC);
        }
        if (node.owner.equals("java/lang/invoke/MethodHandle") &&
            (node.name.equals("invokeExact") || node.name.equals("invoke")) &&
            node.getOpcode() == Opcodes.INVOKEVIRTUAL) {
            // stack - mh, args
            String methodDesc = simplifyDesc(Type.getMethodType(Type.getReturnType(node.desc),
                    Stream.concat(Arrays.stream(new Type[]{
                            Type.getObjectType("java/lang/invoke/MethodHandle")
                    }), Arrays.stream(Type.getArgumentTypes(node.desc))).toArray(Type[]::new)).getDescriptor());
            Type[] methodArguments = Type.getArgumentTypes(methodDesc);
            methodArguments[0] = Type.getObjectType("java/lang/invoke/MethodHandle");
            methodDesc = Type.getMethodDescriptor(Type.getReturnType(methodDesc), methodArguments);
            String mhDesc = simplifyDesc(node.desc);

            HiddenMethodsPool.HiddenMethod hiddenMethod = context.obfuscator.getHiddenMethodsPool()
                    .getMethod("_", methodDesc, method -> {
                        method.visibleAnnotations = new ArrayList<>();
                        method.visibleAnnotations.add(new AnnotationNode("Ljava/lang/invoke/LambdaForm$Hidden;"));
                        method.visibleAnnotations.add(new AnnotationNode("Ljdk/internal/vm/annotation/Hidden;"));
                        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        int index = 1;
                        for (Type argument : Type.getArgumentTypes(mhDesc)) {
                            method.instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), index));
                            index += argument.getSize();
                        }
                        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                "java/lang/invoke/MethodHandle", "invoke", mhDesc));
                        method.instructions.add(new InsnNode(Type.getReturnType(mhDesc).getOpcode(Opcodes.IRETURN)));
                    });

            node = (MethodInsnNode) node.clone(null);
            node.name = hiddenMethod.getMethodNode().name;
            node.owner = hiddenMethod.getClassNode().name;
            node.desc = hiddenMethod.getMethodNode().desc;
            node.setOpcode(Opcodes.INVOKESTATIC);
        }

        Type returnType = Type.getReturnType(node.desc);
        Type[] args = Type.getArgumentTypes(node.desc);
        instructionName += "_" + returnType.getSort();

        StringBuilder argsBuilder = new StringBuilder();
        List<Integer> argOffsets = new ArrayList<>();

        int stackOffset = context.stackPointer;
        for (Type argType : args) {
            stackOffset -= argType.getSize();
        }
        int argumentOffset = stackOffset;
        for (Type argType : args) {
            argOffsets.add(argumentOffset);
            argumentOffset += argType.getSize();
        }

        boolean isStatic = node.getOpcode() == Opcodes.INVOKESTATIC;
        int objectOffset = isStatic ? 0 : 1;

        for (int i = 0; i < argOffsets.size(); i++) {
            argsBuilder.append(", ").append(context.getSnippets().getSnippet("INVOKE_ARG_" + args[i].getSort(),
                    StringUtils.createStringMap("index", argOffsets.get(i))));
        }

        props.put("objectstackindex", String.valueOf(stackOffset - objectOffset));
        props.put("returnstackindex", String.valueOf(stackOffset - objectOffset));

        if (isStatic || node.getOpcode() == Opcodes.INVOKESPECIAL) {
            props.put("class_ptr", context.getCachedClasses().getPointer(node.owner));
        }


        int classId = context.getCachedClasses().getId(node.owner);

        context.output.append(MethodProcessor.getClassCacher(context, classId, node.owner, trimmedTryCatchBlock));

        CachedMethodInfo methodInfo = new CachedMethodInfo(node.owner, node.name, node.desc, isStatic);
        int methodId = context.getCachedMethods().getId(methodInfo);
        props.put("methodid", context.getCachedMethods().getPointer(methodInfo));

        if (isStatic) {
            if (context.manualTryCatch) {
                context.output.append("_CacheStaticMethod(env, ").append(context.getCachedClasses().getId(node.owner)).append(", ").append(methodId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(");");
            } else {
                context.output.append("if(_CacheStaticMethod(env, ").append(context.getCachedClasses().getId(node.owner)).append(", ").append(methodId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(")) ").append(trimmedTryCatchBlock);
            }
        } else {
            if (context.manualTryCatch) {
                context.output.append("_CacheMethod(env, ").append(context.getCachedClasses().getId(node.owner)).append(", ").append(methodId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(");");
            } else {
                context.output.append("if(_CacheMethod(env, ").append(context.getCachedClasses().getId(node.owner)).append(", ").append(methodId).append(", ").append(context.getStringPool().getOffset(node.name)).append(", ").append(context.getStringPool().getOffset(node.desc)).append(")) ").append(trimmedTryCatchBlock);
            }
        }
        props.put("args", argsBuilder.toString());

        if (context.verificationLock != null) {
            context.output.append(context.verificationLock.generateCondition(BufferContext.ConditionType.CODE_BLOCK, context.obfuscator.getSnippets().getSnippet(instructionName, props), "cstack" + props.get("objectstackindex") + " = {};"));
            instructionName = null;
        }
    }

    @Override
    public String insnToString(MethodContext context, MethodInsnNode node) {
        return String.format("%s %s.%s%s", ASMUtils.getOpcodeString(node.getOpcode()), node.owner, node.name, node.desc);
    }

    @Override
    public int getNewStackPointer(MethodInsnNode node, int currentStackPointer) {
        if (node.getOpcode() != Opcodes.INVOKESTATIC) {
            currentStackPointer -= 1;
        }
        return currentStackPointer - Arrays.stream(Type.getArgumentTypes(node.desc)).mapToInt(Type::getSize).sum()
               + Type.getReturnType(node.desc).getSize();
    }
}
