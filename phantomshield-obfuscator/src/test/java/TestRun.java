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
                .setControlFlowObfuscationEnable(false) // control flow
                .addSubFilters("control_flow_obfuscation",
                        "+dev/sim0n/app/test/impl/flow/**",
                        "+dev/sim0n/app/test/impl/flow/** * *(*)")
//                .setInputMappingsFileSetting("mappings.json")
                .setDebugInformationRemoverEnable(false) // Remover
                .setMemberShufflerEnable(false) // Shuffler
                .setRenamerEnable(false) // Renamer
                .addAdaptResources("META-INF/MANIFEST.MF")
                .setRepackageSetting(false)
                .setPrintMappingsSetting(false)
//                .setPrintMappingsFileSetting("mappings.json")
//                .setPrefixName("狼牙")
                .setRepackageNameSetting("skidonion")
                .setStringEncryptionEnable(false) // String
                .setNativeObfuscationEnable(false) // Native
                .setPrintInstructionsSetting(false)
                .setInvokeWrapperEnable(true)
                .addTarget("x86_64-windows")
//                .addSubFilters("native_obfuscation",
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest",
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest * **(**)")

                .build());
        obfuscator.process();
        System.exit(0);
    }
}
