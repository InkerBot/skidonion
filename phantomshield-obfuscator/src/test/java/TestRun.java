import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.ConfigBuilder;

import java.io.File;

public class TestRun {
    public static void main(String[] args) {
        PhantomShield obfuscator = new PhantomShield(new ConfigBuilder()
                .setInputJar(new File("test\\input\\obf-test-1.0-SNAPSHOT.jar"))
                .setOutputJar(new File("test\\output\\obf-test-1.0-SNAPSHOT.jar"))
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar")
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar")
                .setDebugInformationRemoverEnable(true) // Remover
//                .setMemberShufflerEnable(true) // Shuffler
                .setRenamerEnable(true) // Renamer
                .addAdaptResources("META-INF/MANIFEST.MF")
                .setRepackageSetting(true)
                .setPrintMappingsSetting(false)
                .setPrintMappingsFileSetting("mappings.json")
                .setRepackageNameSetting("skidonion")
                .setStringEncryptionEnable(false) // String
                .setNativeObfuscationEnable(false) // Native
                .setInvokedynamicModeSetting("enhancement")
                .addTarget("x86_64-windows")
                .addSubFilter("native_obfuscation", "+dev/sim0n/app/test/impl/evaluation/EvaluationTest")

                .build());
        obfuscator.process();
    }
}
