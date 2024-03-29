import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.ConfigBuilder;
import tech.skidonion.obfuscator.cpp.CompilerUpdater;
import tech.skidonion.obfuscator.inline.Wrapper;

import java.io.File;

public class TestRun {
    public static void main(String[] args) {
        Wrapper._debug_addDefaultCloudConstant("授权验证用户组", "1984756007");
        Wrapper._debug_addDefaultCloudConstant("基础用户组", "108325887");

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
                .setPrintInstructionsSetting(false) //
                .addTarget("x86_64-windows.win7-gnu") //
//                .addTarget("x86_64-linux-gnu") //
//                .addTarget("x86_64-macos") //
//                .addTarget("aarch64-macos") //
                .setVerificationEnableSetting(true)//
                .setVerificationServerSetting("http://localhost:8694/")//
                .setVerificationTokenSetting("fc5c8bf3750cf741378a0c672532583c")//
                .setVerificationUserIdSetting("7")//
                .setVerificationSoftwareIdSetting("1") //
                .setUseInternalUserInterfaceSetting(true)//
                .addSubFilters("native_obfuscation", //
                        "+tech.skidonion.obfuscator.PhantomShield",//
                        "+tech.skidonion.obfuscator.PhantomShield * *(*)",//
                        "+tech.skidonion.obfuscator.transformaer.**",//
                        "+tech.skidonion.obfuscator.transformaer.** * *(*)"//
                )
        ;
    }

    private static void invoke_wrapper(ConfigBuilder builder) {
        builder.setInvokeWrapperEnable(true) //
                .setInjectToOtherClassSetting(false) //
                .setPackageModeSetting("random_existed");
    }

    private static ConfigBuilder basic() {
        return new ConfigBuilder() //
                .setInputJar(new File("test\\input\\obf-test-1.0-SNAPSHOT.jar")) //
                .setOutputJar(new File("test\\output\\obf-test-1.0-SNAPSHOT.jar")) //
//                .setInputJar(new File("test\\input\\dummy.jar")) //
//                .setOutputJar(new File("test\\output\\dummy.jar")) //
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "rt.jar") //
                .addLibrary(System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar");
    }
}
