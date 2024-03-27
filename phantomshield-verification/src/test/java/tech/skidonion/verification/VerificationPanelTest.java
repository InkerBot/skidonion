package tech.skidonion.verification;

import org.junit.jupiter.api.Test;
import tech.skidonion.verification.utils.Internals;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class VerificationPanelTest {
    @Test
    void testSaveInfo() {
        try {
            Path dataPath = Paths.get(System.getProperty("user.home"), "skidonion", "." + Internals.verificationServer().hashCode());
            Files.createDirectories(dataPath);
            try (BufferedWriter writer = Files.newBufferedWriter(dataPath.resolve("userinfo"))) {
                Properties properties = new Properties();
                properties.setProperty("username", "123");
                properties.setProperty("password", "234312321");
                properties.store(writer, "don't leak to anyone^^");
            }
        } catch (Exception ignore) {
        }
    }
}