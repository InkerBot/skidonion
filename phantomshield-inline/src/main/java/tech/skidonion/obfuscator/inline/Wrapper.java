package tech.skidonion.obfuscator.inline;

import java.util.Optional;

public class Wrapper {
    public static Optional<Long> getUserId() {
        return Optional.of(Long.MAX_VALUE);
    }

    public static Optional<String> getUsername() {
        return Optional.of("development");
    }


}
