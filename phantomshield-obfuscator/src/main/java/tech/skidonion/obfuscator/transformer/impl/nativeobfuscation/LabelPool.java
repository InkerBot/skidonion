package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation;

import org.objectweb.asm.Label;

import java.util.Random;
import java.util.WeakHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class LabelPool {
    private final WeakHashMap<Label, Long> labels = new WeakHashMap<>();
    private long currentIndex = 0;

    public String getName(Label label) {
        return "L" + this.labels.computeIfAbsent(label, addedLabel -> ++currentIndex);
    }

    public String randomLabel() {
        return "L" + ThreadLocalRandom.current().nextLong(1, currentIndex + 1);
    }

}
