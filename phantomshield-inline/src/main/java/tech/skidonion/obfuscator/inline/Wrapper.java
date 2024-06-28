package tech.skidonion.obfuscator.inline;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Wrapper {
    private static final Map<Integer, List<String>> DEFAULT_CONSTANT_POOL = new HashMap<>();

    public static void _debug_addDefaultCloudConstant(String role, String constant) {
        DEFAULT_CONSTANT_POOL.compute(role.hashCode(), (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(constant);
            return v;
        });
    }

    public static Optional<Long> getUserId() {
        return Optional.of(Long.MAX_VALUE);
    }

    public static Optional<String> getUsername() {
        return Optional.of("development");
    }

    public static int login(String username, String password, boolean useHashedPassword) {
        return 0;
    }

    public static String getVerifyToken() {
        return "";
    }

    public static void setAsSuspected(String reason) {

    }

    public static Optional<String> getCloudConstant(int hash, int index) {
        String key = "phantom-shield-x.cloud-constant." + hash + "." + index;
        String result;
        if ((result = System.getProperty(key)) != null) {
            return Optional.of(result);
        }

        List<String> array = DEFAULT_CONSTANT_POOL.get(hash);
        if (array != null && index < array.size() && index >= 0) {
            return Optional.of(array.get(index));
        }
        return Optional.empty();
    }

    public static Optional<LocalDateTime> getExpiredDate(String role) {
        return Optional.of(LocalDateTime.now().plusDays(7));
    }

    public static Map<String, LocalDateTime> getExpiredDates() {
        return Collections.emptyMap();
    }

    public static boolean hasRole(String role) {
        return true;
    }

}
