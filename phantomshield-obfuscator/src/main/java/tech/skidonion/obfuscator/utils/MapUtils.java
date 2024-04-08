package tech.skidonion.obfuscator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MapUtils {
    public static <K, V> V computeIfPresent(Map<K, V> map, K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        V oldValue;
        if ((oldValue = Objects.requireNonNull(map).get(key)) != null) {
            V newValue;
            if ((newValue = remappingFunction.apply(key, oldValue)) != null) {
                map.put(key, newValue);
                return newValue;
            } else {
                map.remove(key);
                return null;
            }
        }
        return null;
    }

    public static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        V oldValue;
        if ((oldValue = map.get(key)) == null) {
            V newValue;
            if ((newValue = mappingFunction.apply(key)) != null) {
                map.put(key, newValue);
                return newValue;
            }
        }
        return oldValue;
    }
}
