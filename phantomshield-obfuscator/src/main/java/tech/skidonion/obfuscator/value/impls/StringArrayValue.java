package tech.skidonion.obfuscator.value.impls;

import com.google.gson.JsonElement;
import tech.skidonion.obfuscator.value.Value;

import java.util.List;
import java.util.stream.Collectors;

public class StringArrayValue extends Value<List<String>> {
    public StringArrayValue(String name, List<String> defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public void setValue(JsonElement element) {
        this.setValue(element.getAsJsonArray().asList().stream().map(JsonElement::getAsString).collect(Collectors.toList()));
    }
}
