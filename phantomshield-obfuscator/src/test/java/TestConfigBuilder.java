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
                .setDictionarySetting("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
                .addFilter("-org.example.**")
                .setCreationDateSetting("2024.2.5 12:00:00")
                .setRenamerEnable(true)
                .setPrintMappingsSetting(true)
                .setImportExistingMappingsSetting(true)
                .setInputMappingsFileSetting("mappings.txt")
                .setRepackageSetting(false)
                .setRepackageNameSetting("skidonion/???")
                .addAdaptResources("META-INF/MANIFEST.MF")
                .setStringEncryptionEnable(true)
                .setNativeObfuscationEnable(true)
                .setInvokedynamicModeSetting("enhancement")
                .setLoaderPackageSetting("skidonion/?????")
                .addTarget("x86_64-windows")
                .setPrintInstructionsSetting(false)
                .addSubFilter("native_obfuscation", "+org.example.**")
                .addSubFilter("native_obfuscation", "-org.example.** void main(java.lang.String[])")
                .build()
                .save(new File("config.yaml"));
    }
}
