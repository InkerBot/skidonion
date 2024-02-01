package tech.skidonion.obfuscator.value.impls;

import com.google.gson.JsonElement;
import tech.skidonion.obfuscator.value.Value;

public class StringValue extends Value<String> {

    public StringValue(String name, String defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void setValue(JsonElement element) {
        this.setValue(element.getAsString());
    }
}
