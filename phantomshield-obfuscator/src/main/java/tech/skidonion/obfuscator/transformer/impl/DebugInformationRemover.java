package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.value.impls.BooleanValue;

import java.util.ArrayList;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class DebugInformationRemover extends Transformer {
    private final BooleanValue remove_signatures = new BooleanValue("remove_signatures", true);
    private final BooleanValue remove_source_file = new BooleanValue("remove_source_file", true);
    private final BooleanValue remove_inner_class = new BooleanValue("remove_inner_class", true);
    private final BooleanValue remove_line_number = new BooleanValue("remove_line_number", true);
    private final BooleanValue remove_local_variable = new BooleanValue("remove_local_variable", true);

    public DebugInformationRemover(String name) {
        super(name);
        addSettings(remove_signatures, remove_source_file, remove_inner_class, remove_line_number, remove_local_variable);
    }

    @Override
    public void transform() throws Exception {

        AtomicInteger signatures = new AtomicInteger();
        AtomicInteger inner_class = new AtomicInteger();
        AtomicInteger outer_method = new AtomicInteger();
        AtomicInteger source_file = new AtomicInteger();
        AtomicInteger line_number = new AtomicInteger();
        AtomicInteger local_variable = new AtomicInteger();
        getFilteredClasses().forEach(classWrapper -> {
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
        if (signatures.get() != 0) INFO("Removed {} signatures.", signatures.get());
        if (inner_class.get() != 0) INFO("Removed {} inner classes information.", inner_class.get());
        if (source_file.get() != 0) INFO("Removed {} source file attributes.", source_file.get());
        if (outer_method.get() != 0) INFO("Removed {} outer methods.", outer_method.get());
        if (local_variable.get() != 0) INFO("Removed {} local variables.", local_variable.get());
        if (line_number.get() != 0) INFO("Removed {} line numbers.", line_number.get());
    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }
}
