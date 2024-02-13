package tech.skidonion.obfuscator.value;

import java.util.Objects;

public abstract class Value<V> {
    private final String name;
    private final V defaultValue;
    private V value;

    public Value(String name, V defaultValue) {
        this.name = name;
        this.value = defaultValue;
        this.defaultValue = defaultValue;
    }

    public final String getName() {
        return name;
    }

    public V getValue() {
        return this.value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public abstract void parseConfig(Object element);

    public V getDefaultValue() {
        return defaultValue;
    }

    @Override
    public String toString() {
        return "Value{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof Value)) {
            return false;
        }
        return Objects.hash(this.name, this.value) == Objects.hash(((Value<?>) obj).name, ((Value<?>) obj).value);
    }
}
