package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, Boolean value) {
        super(name, value);
    }

    @Override
    public void parseConfig(Object element) {
        if (element instanceof Boolean) {
            this.setValue(((Boolean) element));
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }

    public boolean isEnable() {
        return this.getValue();
    }

    public void setEnable(boolean enable) {
        this.setValue(enable);
    }
}
