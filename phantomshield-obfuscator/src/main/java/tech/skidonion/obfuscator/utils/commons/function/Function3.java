package tech.skidonion.obfuscator.utils.commons.function;

import org.apache.logging.log4j.util.BiConsumer;

@FunctionalInterface
public interface Function3<V1, V2, V3, R> {
    R apply(V1 v1, V2 v2, V3 v3);
}