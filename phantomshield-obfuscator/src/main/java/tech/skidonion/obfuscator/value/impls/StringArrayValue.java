package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

import java.util.Collections;
import java.util.List;

public class StringArrayValue extends Value<List<String>> {
    public StringArrayValue(String name) {
        this(name, Collections.emptyList());
    }

    public StringArrayValue(String name, List<String> defaultValue) {
        super(name, defaultValue);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parseConfig(Object element) {
        if (element instanceof List<?>) {
            this.setValue((List<String>) element);
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }
}
