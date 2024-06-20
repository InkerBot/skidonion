package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.utils.ASMUtils;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class MemberShuffler extends Transformer {
    public MemberShuffler(String name) {
        super(name);
    }

    @Override
    public void transform() {
        {
            long currentTime = System.currentTimeMillis();
            long seed = obfuscator.getSeed();

            AtomicInteger counter = new AtomicInteger();

            getFilteredClasses().forEach(classWrapper -> {
                Collections.shuffle(classWrapper.getClassNode().methods, new Random(seed));
                Collections.shuffle(classWrapper.getMethods(), new Random(seed));
                counter.addAndGet(classWrapper.getClassNode().methods.size());

                Collections.shuffle(classWrapper.getClassNode().fields, new Random(seed));
                Collections.shuffle(classWrapper.getFields(), new Random(seed));
                counter.addAndGet(classWrapper.getClassNode().fields.size());
            });

            INFO(TRANSLATION("phantom-shield-x.member-shuffler.shuffled"), counter.get(), (System.currentTimeMillis() - currentTime));
        }
    }

    @Override
    public void postprocess() throws Exception {

    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }
}
