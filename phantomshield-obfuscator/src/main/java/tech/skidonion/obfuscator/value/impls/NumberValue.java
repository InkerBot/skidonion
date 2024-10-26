package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

public class NumberValue extends Value<Number> {

    public NumberValue(String name, Number defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void parseConfig(Object element) {
        if (element instanceof Number) {
            this.setValue((Number) element);
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }

}
