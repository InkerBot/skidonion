package tech.skidonion.obfuscator.dictionary;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Dictionary {
    private final String name;
    protected final AtomicInteger uniqueIndex = new AtomicInteger(0);

    public Dictionary(String name) {
        this.name = name;
    }

    public abstract String randomString(int length);

    public abstract String nextUniqueString();

    /**
     * @return reconstruct a new dictionary
     */
    public abstract Dictionary copy();

    public final String getDictionaryName() {
        return this.name;
    }

    public final void setUniqueIndex(int index) {
        this.uniqueIndex.set(index);
    }
}
