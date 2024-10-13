package tech.skidonion.obfuscator.utils.commons.function;

@FunctionalInterface
public interface ThreeParamsReturnFunc<V1, V2, V3, R> {
    R apply(V1 v1, V2 v2, V3 v3);
}
