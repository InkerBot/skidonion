import tech.skidonion.obfuscator.config.ConfigBuilder;

import java.io.File;
import java.io.IOException;

public class TestConfigBuilder {
    public static void main(String[] args) throws IOException {
        new ConfigBuilder()
                .setInputJar(new File("test\\input\\obf-test-1.0-SNAPSHOT.jar"))
                .setOutputJar(new File("test\\output\\obf-test-1.0-SNAPSHOT.jar"))
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar")
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar")
                .setModeInvokeDynamicNativeConverter("enhancement")
                .setCreationDate("2024.2.5 12:00:00")
                .addFilter("-org.example.**")
                .addSubFilter("native_obfuscation", "+org.example.**")
                .addSubFilter("native_obfuscation", "-org.example.** void main(java.lang.String[])")
                .setPrintInstructions(false)
                .setLoaderDirectory("skidonion/?????")
                .addTarget("x86_64-windows")
                .setStringObfuscation(true)
                .setNativeObfuscation(true)
                .build()
                .save(new File("config.json"));
    }
}
