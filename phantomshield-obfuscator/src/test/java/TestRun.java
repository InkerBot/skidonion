import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.ConfigBuilder;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.utils.commons.UTF8Control;

import java.io.File;
import java.nio.file.Paths;
import java.util.ResourceBundle;

public class TestRun {
    public static void main(String[] args) throws Exception {
        if (PhantomShield.BUNDLE == null) {
            PhantomShield.BUNDLE = ResourceBundle.getBundle("i18n.lang", new UTF8Control());
        }

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
        control_flow(builder);
//        native_obfuscation(builder);
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
                .setRepackageSetting(true) //
                .setRepackageNameSetting("skidonion") //
                .setClassPrefixNameSetting("Class_") //
                .setMethodPrefixNameSetting("method_") //
                .setFieldPrefixNameSetting("field_") //
                .setDictionarySetting("abcdefghijklmnopqrstuvwxyz") //
                .setMinimumGeneratedNameLengthSetting(1) //
                .setPrintMappingsSetting(false) //
                .setPrintMappingsFileSetting("mappings.json") //
//                .setInputMappingsFileSetting("mappings.json") //
                .addAdaptResources("META-INF/MANIFEST.MF") //
                .setMixinSupportSetting(false) //
                .setMixinsJsonSetting("liquidbounce.forge.mixins.json") //
                .setMixinsRefJsonSetting("liquidbounce.mixins.refmap.json") //
                .addSubFilters("renamer", "-com/gmail/olexorus/themis/api/**") //
        ;
    }

    private static void string_encryption(ConfigBuilder builder) {
        builder.setStringEncryptionEnable(true);
    }

    private static void native_obfuscation(ConfigBuilder builder) {
        builder.setNativeObfuscationEnable(true) //
                .setPrintInstructionsSetting(true) //
                .addTarget("x86_64-windows-gnu") //
//                .setLegacyCompileModeSetting(false)//
                .setNullSafetySetting(true)//
//                .addTarget("x86_64-linux-gnu") //
//                .addTarget("x86_64-macos") //
//                .addTarget("aarch64-macos") //
                .setVerificationEnableSetting(true)//
//                .setVerificationServerSetting("http://localhost:8694/")//
                .setVerificationTokenSetting("769e4f678db8436b0018fc6fe60a5a7a")//
                .setVerificationUserIdSetting("1")//
                .setVerificationSoftwareIdSetting("1") //
                .setUseInternalUserInterfaceSetting(true)//
                .addSubFilters("native_obfuscation",
                        "+pack.**",
                        "+pack.** * *(*)")
//                .addSubFilters("native_obfuscation", //
//                        "+pack.Clazz",//
//                        "+pack.Clazz * *(*)",//
//                        "+pack.tests.basics.**",//
//                        "+pack.tests.basics.** * *(*)",//
//                        "+pack.tests.bench.Calc",//
//                        "+pack.tests.bench.Calc * *(*)"
//                )
        ;
    }

    private static void invoke_wrapper(ConfigBuilder builder) {
        builder.setInvokeWrapperEnable(true) //
                .setInjectToOtherClassSetting(false) //
                .setPackageModeSetting("random_existed");
    }

    private static ConfigBuilder basic() {
        return new ConfigBuilder() //
                .setGeneratePhantomClassesSetting(true) //
                .setPrintClassesAsDirectorySetting(false)//
                .setInputJar(new File("test/input/obf-test-1.0-SNAPSHOT.jar")) //
                .setOutputJar(new File("test/output/bench.jar")) //
//                 .setLegacyCompileModeSetting(true)
                .addLibrary(Paths.get("test", "libs").toString())
                .addLibrary(System.getProperty("java.home") + File.separator + "jmods") // java 9+
                .addLibrary(System.getProperty("java.home") + File.separator + "lib") // java 8
//                .addSoftExclusions("-net.ccbluex.liquidbounce.**")
                ;
    }
}
