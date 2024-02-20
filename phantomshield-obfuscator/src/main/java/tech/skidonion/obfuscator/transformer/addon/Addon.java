package tech.skidonion.obfuscator.transformer.addon;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public abstract class Addon {
    protected PhantomShield obfuscator;

    public void init(PhantomShield obfuscator) {
        this.obfuscator = obfuscator;
    }

    public abstract void transform();

    protected final Map<String, ClassWrapper> getClasses() {
        return this.obfuscator.classes;
    }

    protected final Collection<ClassWrapper> getClassWrappers() {
        return this.obfuscator.classes.values();
    }

    protected final Map<String, ClassWrapper> getClassPath() {
        return this.obfuscator.classpath;
    }

    protected final Map<String, byte[]> getResources() {
        return this.obfuscator.resources;
    }

    protected String randomClassName() {
        Collection<String> classNames = getClasses().keySet();
        ArrayList<String> list = new ArrayList<>(classNames);

        String first = list.get(RandomUtils.getRandomInt(classNames.size()));
        String second = list.get(RandomUtils.getRandomInt(classNames.size()));

        return first + '$' + second.substring(second.lastIndexOf("/") + 1);
    }
}
