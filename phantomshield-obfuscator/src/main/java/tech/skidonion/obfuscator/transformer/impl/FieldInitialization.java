package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.utils.ASMUtils;

import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class FieldInitialization extends Transformer {
    public FieldInitialization(String name) {
        super(name, true);
    }

    @Override
    public void preprocess() throws Exception {
        {
            long currentTime = System.currentTimeMillis();
            AtomicInteger counter = new AtomicInteger();

            getFilteredClasses().forEach(classWrapper -> {
                MethodNode clinit = classWrapper.getOrCreateClinit();

                classWrapper.getFields().stream().filter(fieldWrapper -> Modifier.isStatic(fieldWrapper.getFieldNode().access)
                        && fieldWrapper.getFieldNode().value != null
                        && match(fieldWrapper)).forEach(fieldWrapper -> {
                    FieldNode fieldNode = fieldWrapper.getFieldNode();
                    Object val = fieldNode.value;

                    exit:
                    {
                        InsnList toAdd = new InsnList();

                        if (val instanceof String)
                            toAdd.insert(new LdcInsnNode(val));
                        else if (val instanceof Integer)
                            toAdd.insert(ASMUtils.getNumberInsn((Integer) val));
                        else if (val instanceof Long)
                            toAdd.insert(ASMUtils.getNumberInsn((Long) val));
                        else if (val instanceof Float)
                            toAdd.insert(ASMUtils.getNumberInsn((Float) val));
                        else if (val instanceof Double)
                            toAdd.insert(ASMUtils.getNumberInsn((Double) val));
                        else
                            break exit;

                        toAdd.add(new FieldInsnNode(PUTSTATIC, classWrapper.getName(), fieldNode.name, fieldNode.desc));
                        clinit.instructions.insert(toAdd);
                        fieldNode.value = null;

                        counter.incrementAndGet();
                    }
                });
            });

            INFO(TRANSLATION("phantom-shield-x.member-shuffler.moved"), counter.get(), (System.currentTimeMillis() - currentTime));
        }
    }

    @Override
    public void transform() throws Exception {

    }

    @Override
    public void postprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return "";
    }
}
