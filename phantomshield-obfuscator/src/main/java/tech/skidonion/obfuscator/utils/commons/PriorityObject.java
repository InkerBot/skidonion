package tech.skidonion.obfuscator.utils.commons;

import java.util.Objects;

public class PriorityObject<V> implements Comparable<PriorityObject<?>> {
    private final V object;
    private final int priority;

    public PriorityObject(V object, int priority) {
        this.object = object;
        this.priority = priority;
    }

    @Override
    public int compareTo(PriorityObject other) {
        return this.priority - other.priority;
    }

    public V getObject() {
        return object;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return object.toString();
    }

    @Override
    public int hashCode() {
        return object.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PriorityObject) {
            return Objects.equals(this.object, ((PriorityObject<?>) obj).getObject());
        }
        return false;
    }
}