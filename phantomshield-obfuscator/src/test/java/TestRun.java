import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.ConfigBuilder;

import java.io.File;

public class TestRun {
    public static void main(String[] args) {
//        CompilerUpdater.updateCompiler();
        ConfigBuilder builder = basic();
//         =================
//        debug_information_remover(builder);
//        shuffler(builder);
//        renamer(builder);
//        string_encryption(builder);
//        invoke_wrapper(builder);
//        control_flow(builder);
        native_obfuscation(builder);
//         =================
        new PhantomShield(builder.build()).process();
        System.exit(0);
    }

    private static void control_flow(ConfigBuilder builder) {
        builder.setControlFlowObfuscationEnable(true)
//                .addSubFilters("control_flow_obfuscation", //
//                        "+dev/sim0n/app/test/impl/flow/WeirdLoopTest", //
//                        "+dev/sim0n/app/test/impl/flow/WeirdLoopTest * *(*)") //
//                .addSubFilters("control_flow_obfuscation", //
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest", //
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest * *(*)") //
        ;
    }

    private static void debug_information_remover(ConfigBuilder builder) {
        builder.setDebugInformationRemoverEnable(true);
    }

    private static void shuffler(ConfigBuilder builder) {
        builder.setMemberShufflerEnable(true);
    }

    private static void renamer(ConfigBuilder builder) {
        builder.setRenamerEnable(true) //
                .setRepackageSetting(false) //
                .setRepackageNameSetting("skidonion") //
                .setPrefixNameSetting("狼牙") //
                .setPrintMappingsSetting(false) //
                .setPrintMappingsFileSetting("mappings.json") //
//                .setInputMappingsFileSetting("mappings.json") //
                .addAdaptResources("META-INF/MANIFEST.MF");
    }

    private static void string_encryption(ConfigBuilder builder) {
        builder.setStringEncryptionEnable(true);
    }

    private static void native_obfuscation(ConfigBuilder builder) {
        builder.setNativeObfuscationEnable(true) //
                .setPrintInstructionsSetting(true) //
                .addTarget("x86_64-windows") //
//                .addTarget("x86_64-linux-gnu") //
//                .addTarget("x86_64-macos") //
//                .addTarget("aarch64-macos") //
                .setVerificationEnableSetting(true)//
                .setVerificationTokenSetting("")//
                .setUseInternalUserInterfaceSetting(true)//
//                .addSubFilters("native_obfuscation", //
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest", //
//                        "+dev/sim0n/app/test/impl/evaluation/EvaluationTest * **(**)")
        ;
    }

    private static void invoke_wrapper(ConfigBuilder builder) {
        builder.setInvokeWrapperEnable(true) //
                .setInjectToOtherClassSetting(false) //
                .setPackageModeSetting("random_existed");
    }

    private static ConfigBuilder basic() {
        return new ConfigBuilder() //
//                .setInputJar(new File("test\\input\\obf-test-1.0-SNAPSHOT.jar")) //
//                .setOutputJar(new File("test\\output\\obf-test-1.0-SNAPSHOT.jar")) //
                .setInputJar(new File("test\\input\\dummy.jar")) //
                .setOutputJar(new File("test\\output\\dummy.jar")) //
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar") //
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar");
    }
}
