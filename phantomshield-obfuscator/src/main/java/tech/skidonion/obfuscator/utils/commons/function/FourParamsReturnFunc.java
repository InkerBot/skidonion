package tech.skidonion.obfuscator.utils.commons.function;

@FunctionalInterface
public interface FourParamsReturnFunc<V1, V2, V3, V4, R> {
    R apply(V1 v1, V2 v2, V3 v3, V4 v4);
}
