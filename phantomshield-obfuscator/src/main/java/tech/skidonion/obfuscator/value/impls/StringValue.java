package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

public class StringValue extends Value<String> {

    public StringValue(String name, String defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void parseConfig(Object element) {
        if (element instanceof String) {
            this.setValue((String) element);
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }
}
