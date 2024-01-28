package tech.skidonion.obfuscator.value.impls;

import com.google.gson.JsonElement;
import tech.skidonion.obfuscator.value.Value;

public class ModeValue extends Value<String> {
    private final String[] modes;

    public ModeValue(String name, String defaultValue, String... modes) {
        super(name, defaultValue);
        this.modes = modes;
    }

    @Override
    public void setValue(JsonElement element) {
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            if (is(value)) {
                this.setValue(value);
            }
        }
    }

    public String[] getModes() {
        return modes;
    }

    public boolean is(String mode) {
        for (String m : modes) {
            if (m.equals(mode)) {
                return true;
            }
        }
        return false;
    }

}
