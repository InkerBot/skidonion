package tech.skidonion.obfuscator.transformer.generic;

import org.objectweb.asm.Type;

import java.util.*;

public class StackCodeBlockMap {
    private final Map<Stack, List<CodeBlock>> map = new HashMap<>();

    public void add(final Stack stack, final CodeBlock block) {
        map.compute(stack, (k, v) -> {
            if (v == null) v = new ArrayList<>();
            v.add(block);
            return v;
        });
    }

    public List<CodeBlock> get(final Stack stack) {
        return map.get(stack);
    }

    public static class Stack {
        public final Type[] types;

        public Stack(Type... types) {
            this.types = types;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) return true;
            if (object == null || getClass() != object.getClass()) return false;
            Stack stack = (Stack) object;
            return Arrays.equals(types, stack.types);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(types);
        }
    }
}
