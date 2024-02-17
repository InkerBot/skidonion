package tech.skidonion.obfuscator.transformer.impl.renamer;

import java.util.HashMap;
import java.util.Map;

public class RenamerResult {
    private String obfuscatedName;
    private final Map<String, RenamerType> influences = new HashMap<>();
    private int maximumIndex;

    public String getObfuscatedName() {
        return obfuscatedName;
    }

    public RenamerType add(String key, RenamerType value) {
        return influences.put(key, value);
    }

    public void setObfuscatedName(String obfuscatedName) {
        this.obfuscatedName = obfuscatedName;
    }

    public int getMaximumIndex() {
        return maximumIndex;
    }

    public void setMaximumIndex(int maximumIndex) {
        this.maximumIndex = maximumIndex;
    }

    public Map<String, RenamerType> getInfluences() {
        return influences;
    }

    public enum RenamerType {
        FIELD, METHOD, ANNOTATION, DUMMY;
    }
}
