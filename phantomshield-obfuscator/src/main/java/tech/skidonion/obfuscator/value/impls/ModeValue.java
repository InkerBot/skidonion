package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

import java.util.Objects;

public class ModeValue extends Value<String> {
    private final String[] modes;

    public ModeValue(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    @Override
    public void parseConfig(Object element) {
        if (element instanceof String) {
            String value = ((String) element);
            if (has(value)) {
                this.setValue(value);
                return;
            }
            throw new UnsupportedOperationException(value + " is not existed in " + getName());
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }

    public String[] getModes() {
        return modes;
    }

    public boolean is(String mode) {
        return Objects.requireNonNull(mode).equals(getValue());
    }

    public boolean has(String mode) {
        for (String m : modes) {
            if (m.equals(mode)) {
                return true;
            }
        }
        return false;
    }

}
