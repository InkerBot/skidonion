package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.value.impls.BooleanValue;

import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class DebugInformationRemover extends Transformer {
    private final BooleanValue remove_signatures = new BooleanValue("remove_signatures", true);

    public DebugInformationRemover(String name) {
        super(name);
    }

    @Override
    public void transform() throws Exception {
        remove_signatures:
        {
            if (!remove_signatures.isEnable()) break remove_signatures;
            AtomicInteger counter = new AtomicInteger();

            getFilteredClasses().forEach(classWrapper -> {
                ClassNode classNode = classWrapper.getClassNode();

                if (classNode.signature != null) {
                    classNode.signature = null;
                    counter.incrementAndGet();
                }

                classWrapper.getMethods().stream().filter(methodWrapper -> match(methodWrapper)
                        && methodWrapper.getMethodNode().signature != null).forEach(methodWrapper -> {
                    methodWrapper.getMethodNode().signature = null;
                    counter.incrementAndGet();
                });

                classWrapper.getFields().stream().filter(fieldWrapper -> match(fieldWrapper)
                        && fieldWrapper.getFieldNode().signature != null).forEach(fieldWrapper -> {
                    fieldWrapper.getFieldNode().signature = null;
                    counter.incrementAndGet();
                });
            });

            INFO("Removed {} signatures.", counter.get());
        }

    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }
}
