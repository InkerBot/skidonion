package tech.skidonion.obfuscator.value.impls;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import tech.skidonion.obfuscator.value.Value;

public class BooleanValue extends Value<Boolean> {
    public BooleanValue(String name, Boolean value) {
        super(name, value);
    }

    @Override
    public void setValue(JsonElement element) {
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isBoolean()) {
            this.setValue(primitive.getAsBoolean());
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
