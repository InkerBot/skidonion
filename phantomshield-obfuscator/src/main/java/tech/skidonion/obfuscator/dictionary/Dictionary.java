package tech.skidonion.obfuscator.dictionary;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Dictionary {
    private String name;
    protected final AtomicInteger uniqueIndex = new AtomicInteger(0);
    protected final AtomicInteger offset = new AtomicInteger(0);

    public Dictionary(String name) {
        this.name = name;
    }

    /**
     * @return reconstruct a new dictionary
     */
    public abstract Dictionary copy();

    public abstract String next();

    public abstract int size();

    public abstract String generate(int index);

    public final String getDictionaryName() {
        return this.name;
    }

    public final void setUniqueIndex(int index) {
        this.uniqueIndex.set(index);
    }

    public final int getUniqueIndex() {
        return this.uniqueIndex.get();
    }

    public AtomicInteger getOffset() {
        return offset;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
