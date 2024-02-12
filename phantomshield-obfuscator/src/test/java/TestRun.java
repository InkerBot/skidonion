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
                // TODO
                .setRenamer(true)
                .addAdaptResources("META-INF/MANIFEST.MF")
                .setImportExistingMappings(true)
                .setInputMappingsFile("mappings.txt")
                .setRepackage(false)
                .setPrintMappings(false)
                .setPrintMappingsFile("mappings.txt")
                .setRepackageName("skidonion")
                // TODO
                .setStringEncryption(true)
                // TODO
                .setNativeObfuscation(false)
                .setModeInvokeDynamicNativeConverter("enhancement")
                .addTarget("x86_64-windows")
                .addSubFilter("native_obfuscation", "-** void main(java.lang.String[])")

                .build());
        obfuscator.process();
    }
}
