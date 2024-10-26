package tech.skidonion.obfuscator.value.impls;

import lombok.val;
import tech.skidonion.obfuscator.utils.commons.Pair;
import tech.skidonion.obfuscator.value.Value;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RangeValue extends Value<Pair<Integer, Integer>> {
    public RangeValue(String name, Integer min, Integer max) {
        super(name, new Pair<>(min, max));
    }

    @Override
    public void parseConfig(Object element) {
        boolean isArray = element instanceof int[];
        if (element instanceof List || isArray) {
            List<?> value;
            if (isArray) {
                value = Arrays.stream(((int[]) element)).boxed().collect(Collectors.toList());
            } else {
                value = ((List<?>) element);
            }
            if (value.size() != 2) {
                throw new IllegalArgumentException("Range value only has two elements in " + this.getName());
            }
            val first = value.get(0);
            val second = value.get(1);
            if (first instanceof Integer && second instanceof Integer) {
                val min = (Integer) first;
                val max = (Integer) second;
                if (min > max) {
                    throw new IllegalArgumentException("min value can't greater than max in" + this.getName());
                }
                setValue(new Pair<>(min, max));
                return;
            } else {
                throw new IllegalArgumentException("Range value can only be NUMBER in " + this.getName());
            }
        } else if (element instanceof Integer) {
            val value = (Integer) element;
            this.setValue(new Pair<>(value, value));
            return;
        }
        throw new IllegalArgumentException("Invalid Config Type in " + this.getName());
    }

    public int getRandomValue() {
        return ThreadLocalRandom.current().nextInt(this.getValue().getFirst(), this.getValue().getSecond() + 1);
    }

}
