package tech.skidonion.obfuscator.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class MapUtils {
    public static <K, V> V computeIfPresent(Map<K, V> map, K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        if (Objects.requireNonNull(map).get(key) != null) {
            V oldValue = map.get(key);
            V newValue = remappingFunction.apply(key, oldValue);
            if (newValue != null) map.put(key, newValue);
            else map.remove(key);
        }
        return null;
    }

    public static <K, V> V computeIfAbsent(Map<K, V> map, K key, Function<? super K, ? extends V> mappingFunction) {
        if (map.get(key) == null) {
            V newValue = mappingFunction.apply(key);
            if (newValue != null)
                map.put(key, newValue);
        }
        return null;
    }
}
