package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.value.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SubValue extends Value<Map<String, Value<?>>> {

    public SubValue(String name) {
        super(name, Collections.emptyMap());
    }

    public SubValue(String name, Value<?>... values) {
        this(name);
        Map<String, Value<?>> map = new HashMap<>();
        for (Value<?> value : values) {
            map.put(value.getName(), value);
        }
        setValue(map);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void parseConfig(Object element) {
        if (element instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) element;
            for (Value<?> value : getValue().values()) {
                Object val = map.get(value.getName());
                if (val != null) {
                    value.parseConfig(val);
                }
            }
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }
}
