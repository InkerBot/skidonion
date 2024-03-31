package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.asm.InstructionModifier;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.value.impls.BooleanValue;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class DebugInformationRemover extends Transformer {
    private final BooleanValue remove_signatures = new BooleanValue("remove_signatures", true);
    private final BooleanValue remove_source_file = new BooleanValue("remove_source_file", true);
    private final BooleanValue remove_inner_class = new BooleanValue("remove_inner_class", true);
    private final BooleanValue remove_line_number = new BooleanValue("remove_line_number", true);
    private final BooleanValue remove_local_variable = new BooleanValue("remove_local_variable", true);
    private final BooleanValue remove_kotlin_reference = new BooleanValue("remove_kotlin_reference", true);

    public DebugInformationRemover(String name) {
        super(name);
        addSettings(remove_signatures, remove_source_file, remove_inner_class, remove_line_number, remove_local_variable, remove_kotlin_reference);
    }

    @Override
    public void transform() throws Exception {

        AtomicInteger signatures = new AtomicInteger();
        AtomicInteger inner_class = new AtomicInteger();
        AtomicInteger outer_method = new AtomicInteger();
        AtomicInteger source_file = new AtomicInteger();
        AtomicInteger line_number = new AtomicInteger();
        AtomicInteger local_variable = new AtomicInteger();
        AtomicInteger kotlin_reference = new AtomicInteger();
        getFilteredClasses().forEach(classWrapper -> {
            remove_kotlin_reference:
            {
                if (!remove_kotlin_reference.isEnable()) break remove_kotlin_reference;
                ClassNode classNode = classWrapper.getClassNode();
                if (classNode.visibleAnnotations != null) {
                    ListIterator<AnnotationNode> it = classNode.visibleAnnotations.listIterator();
                    while (it.hasNext()) {
                        AnnotationNode annotation = it.next();
                        if (annotation.desc.equals("Lkotlin/Metadata;") || annotation.desc.equals("Lkotlin/coroutines/jvm/internal/DebugMetadata;")) {
                            it.remove();
                            kotlin_reference.incrementAndGet();
                        }
                    }
                }

                for (MethodNode method : classNode.methods) {
                    InstructionModifier modifier = new InstructionModifier();
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof MethodInsnNode && ((MethodInsnNode) insn).owner.equals("kotlin/jvm/internal/Intrinsics")) {
                            MethodInsnNode methodInsn = (MethodInsnNode) insn;
                            if (methodInsn.name.equals("checkParameterIsNotNull")) {
                                if (methodInsn.desc.equals("(Ljava/lang/Object;Ljava/lang/String;)V")) {
                                    AbstractInsnNode prev = insn.getPrevious();
                                    if (prev instanceof LdcInsnNode) {
                                        LdcInsnNode ldcInsn = (LdcInsnNode) prev;
                                        modifier.remove(ldcInsn);
                                        modifier.replace(methodInsn, new InsnNode(POP));
                                        kotlin_reference.incrementAndGet();
                                    } else {
                                        InsnList list = new InsnList();
                                        list.add(new InsnNode(POP));
                                        list.add(new InsnNode(POP));
                                        modifier.replace(methodInsn, list);
                                        kotlin_reference.incrementAndGet();
                                    }
                                } else {
                                    modifier.replace(methodInsn, new InsnNode(POP));
                                }
                            } else if (methodInsn.name.equals("checkExpressionValueIsNotNull")) {
                                AbstractInsnNode prev = insn.getPrevious();
                                if (prev instanceof LdcInsnNode) {
                                    LdcInsnNode ldcInsn = (LdcInsnNode) prev;
                                    modifier.remove(ldcInsn);
                                    modifier.replace(methodInsn, new InsnNode(POP));
                                    kotlin_reference.incrementAndGet();
                                }
                            }
                        }
                    }
                    modifier.apply(method);
                }
            }

            remove_signatures:
            {
                if (!remove_signatures.isEnable()) break remove_signatures;
                ClassNode classNode = classWrapper.getClassNode();

                if (classNode.signature != null) {
                    classNode.signature = null;
                    signatures.incrementAndGet();
                }

                classWrapper.getMethods().stream().filter(methodWrapper -> match(methodWrapper) && methodWrapper.getMethodNode().signature != null).forEach(methodWrapper -> {
                    methodWrapper.getMethodNode().signature = null;
                    signatures.incrementAndGet();
                });

                classWrapper.getFields().stream().filter(fieldWrapper -> match(fieldWrapper) && fieldWrapper.getFieldNode().signature != null).forEach(fieldWrapper -> {
                    fieldWrapper.getFieldNode().signature = null;
                    signatures.incrementAndGet();
                });
            }
            remove_inner_class:
            {
                if (!remove_inner_class.isEnable()) break remove_inner_class;

                if (classWrapper.getClassNode().outerClass != null) {
                    classWrapper.getClassNode().outerClass = null;
                    classWrapper.getClassNode().outerMethod = null;
                    classWrapper.getClassNode().outerMethodDesc = null;
                    outer_method.incrementAndGet();
                }
                if (classWrapper.getClassNode().innerClasses != null) {
                    inner_class.addAndGet(classWrapper.getClassNode().innerClasses.size());
                    classWrapper.getClassNode().innerClasses = new ArrayList<>();
                }
            }
            remove_source_file:
            {
                if (!remove_source_file.isEnable()) break remove_source_file;
                if (classWrapper.getClassNode().sourceFile != null) {
                    classWrapper.getClassNode().sourceFile = null;
                    source_file.incrementAndGet();
                }
            }
            classWrapper.getMethods().stream().filter(this::match).forEach(methodWrapper -> {
                remove_signatures:
                {
                    if (!remove_signatures.isEnable()) break remove_signatures;
                    if (methodWrapper.getMethodNode().signature != null) {
                        methodWrapper.getMethodNode().signature = null;
                        signatures.incrementAndGet();
                    }
                }
                remove_local_variable:
                {
                    if (!remove_local_variable.isEnable()) break remove_local_variable;
                    if (methodWrapper.getMethodNode().localVariables != null) {
                        local_variable.addAndGet(methodWrapper.getMethodNode().localVariables.size());
                        methodWrapper.getMethodNode().localVariables = null;
                    }

                }

                remove_line_number:
                {
                    if (!remove_line_number.isEnable()) break remove_line_number;
                    MethodNode methodNode = methodWrapper.getMethodNode();

                    for (ListIterator<AbstractInsnNode> it = methodNode.instructions.iterator(); it.hasNext(); ) {
                        AbstractInsnNode insn = it.next();
                        if (insn instanceof LineNumberNode) {
                            it.remove();
                            line_number.incrementAndGet();
                        }
                    }
                }
            });
            classWrapper.getFields().stream().filter(this::match).forEach(fieldWrapper -> {
                remove_signatures:
                {
                    if (!remove_signatures.isEnable()) break remove_signatures;
                    if (fieldWrapper.getFieldNode().signature != null) {
                        fieldWrapper.getFieldNode().signature = null;
                        signatures.incrementAndGet();
                    }
                }
            });

        });
        if (signatures.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed1"), signatures.get());
        if (inner_class.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed2"), inner_class.get());
        if (source_file.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed3"), source_file.get());
        if (outer_method.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed4"), outer_method.get());
        if (local_variable.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed5"), local_variable.get());
        if (line_number.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed6"), line_number.get());
        if (kotlin_reference.get() != 0) INFO(TRANSLATION("phantom-shield-x.debug-information-remover.removed7"), kotlin_reference.get());
    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }
}
