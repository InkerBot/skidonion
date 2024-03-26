package tech.skidonion.obfuscator.inline;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

public class Wrapper {
    public static Optional<Long> getUserId() {
        return Optional.of(Long.MAX_VALUE);
    }

    public static Optional<String> getUsername() {
        return Optional.of("development");
    }

    public static int login(String username, String password) {
        return 0;
    }

    public static void setAsSuspected(String reason) {

    }

    public static Optional<String> getCloudConstant(int hash, int index) {
        return Optional.of("constant");
    }

    public static Optional<LocalDateTime> getExpiredDate(String role) {
        return Optional.of(LocalDateTime.now());
    }

    public static Map<String, LocalDateTime> getExpiredDates() {
        return Collections.emptyMap();
    }

    public static boolean hasRole(String role) {
        return true;
    }

}
