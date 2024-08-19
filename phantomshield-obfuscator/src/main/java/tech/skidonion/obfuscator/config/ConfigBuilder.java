package tech.skidonion.obfuscator.config;

import java.io.File;
import java.util.*;

public class ConfigBuilder {

    // attributions
    private boolean debugSetting;
    private File inputJarSetting;
    private File outputJarSetting;
    private boolean generatePhantomClassesSetting = false;
    private String creationDateSetting;
    private String cppCompilerSetting;
    private String cppCompilerArgumentsSetting;
    private String cppCompilerOutputSetting;
    private boolean cppCompilerIsAarch64 = false;
    private long randomSeedSetting;
    private boolean printClassesAsDirectorySetting = false;
    private String dictionarySetting = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private long minimumGeneratedNameLengthSetting = 1;
    private String inputMappingsFileSetting;
    private final List<String> targetsSettings = new ArrayList<>();
    private final List<String> librariesSettings = new ArrayList<>();
    private final List<String> filtersSettings = new ArrayList<>();

    // sub filters
    private final Map<String, List<String>> subFiltersSettings = new HashMap<>();

    // ======= transformers settings =======
    // field initialization
    private boolean fieldInitializationSetting = false;

    // string encryption
    private boolean stringEncryptionEnable = false;

    // native obfuscation
    private boolean nativeObfuscationEnable = false;
    private String loaderPackageSetting = "skidonion/??????";
    private boolean printInstructionsSetting = false;
    private boolean hiddenStackTraceSetting = true;
    private boolean nullSafetySetting = false;
    private boolean verificationEnableSetting = false;
    private boolean useInternalUserInterfaceSetting = true;
    private boolean verificationKeepAliveSetting = true;
    private String verificationServerSetting = "https://skidonion.tech/";
    private String verificationUserIdSetting = "-1";
    private String verificationSoftwareIdSetting = "-1";
    private String verificationTokenSetting = "";

    // renamer
    private boolean renamerEnable = false;
    private boolean repackageSetting = false;
    private String repackageNameSetting = "skidonion/??????";
    private boolean printMappingsSetting = false;
    private String printMappingsFileSetting = "mappings.txt";
    private String prefixNameSetting = "";
    private final List<String> adaptResourcesSetting = new ArrayList<>();
    private boolean mixinsSupportSetting = false;
    private String mixinsJsonSetting = "mixins.json";
    private String mixinsRefJsonSetting = "mixins.ref.json";

    // member shuffler
    private boolean memberShufflerEnable = false;

    // debug_information_remover
    private boolean debugInformationRemoverEnable = false;
    private boolean removeSignaturesSetting = true;
    private boolean removeSourceFileSetting = true;
    private boolean removeInnerClassSetting = true;
    private boolean removeLineNumberSetting = true;
    private boolean removeLocalVariableSetting = true;
    private boolean removeKotlinReferenceSetting = true;

    // control flow obfuscation
    private boolean controlFlowObfuscationEnable = false;

    // invoke wrapper
    private boolean invokeWrapperEnable = false;
    private boolean injectToOtherClassSetting = true;
    private String packageModeSetting = "";

    public final Config build() {
        Config config = new Config();
        config.add("input", Objects.requireNonNull(inputJarSetting, "input is null").getAbsoluteFile().toString());
        config.add("output", Objects.requireNonNull(outputJarSetting, "output is null").getAbsoluteFile().toString());
        config.add("dictionary", dictionarySetting);
        config.add("minimum_generated_name_length", minimumGeneratedNameLengthSetting);

        if (debugSetting) {
            config.add("__debug", true);
        }

        if (generatePhantomClassesSetting) {
            config.add("generate_phantom_classes", true);
        }

        if (creationDateSetting != null) {
            config.add("creation_date", creationDateSetting);
        }

        if (randomSeedSetting != 0) {
            config.add("random_seed", randomSeedSetting);
        }

        if (printClassesAsDirectorySetting) {
            config.add("print_classes_as_directory", true);
        }

        if (inputMappingsFileSetting != null) {
            config.add("input_mappings_file", inputMappingsFileSetting);
        }

        if (cppCompilerSetting != null) {
            config.add("cpp_compiler", cppCompilerSetting);
        }

        if (cppCompilerArgumentsSetting != null) {
            config.add("cpp_compiler_arguments", cppCompilerArgumentsSetting);
        }

        if (cppCompilerOutputSetting != null) {
            config.add("cpp_compiler_output", cppCompilerOutputSetting);
        }

        if (cppCompilerIsAarch64) {
            config.add("cpp_compiler_is_aarch64", true);
        }

//        if (legacyCompileModeSetting) {
//            config.add("legacy_compile_mode", true);
//        }

        if (!targetsSettings.isEmpty()) {
            config.add("targets", targetsSettings);
        }

        if (!librariesSettings.isEmpty()) {
            config.add("libraries", librariesSettings);
        }

        if (!filtersSettings.isEmpty()) {
            config.add("filters", filtersSettings);
        }


        // 处理变压器的方法
        native_obfuscation:
        {
            // 如果不开启则直接跳过代码块
            if (!nativeObfuscationEnable) break native_obfuscation;
            // 生成一个子json对象
            Map<String, Object> native_obfuscation = new LinkedHashMap<>();

            // 添加 settings
            native_obfuscation.put("loader_package", loaderPackageSetting);
            native_obfuscation.put("print_instructions", printInstructionsSetting);
            native_obfuscation.put("hidden_stack_trace", hiddenStackTraceSetting);
            native_obfuscation.put("null_safety", nullSafetySetting);

            Map<String, Object> verification = new LinkedHashMap<>();
            verification.put("verification_enable", verificationEnableSetting);
            verification.put("use_internal_user_interface", useInternalUserInterfaceSetting);
            verification.put("verification_server", verificationServerSetting);
            verification.put("verification_software_id", verificationSoftwareIdSetting);
            verification.put("verification_user_id", verificationUserIdSetting);
            verification.put("verification_token", verificationTokenSetting);
            verification.put("verification_keep_alive", verificationKeepAliveSetting);


            native_obfuscation.put("verification", verification);

            // 添加 过滤器
            subFiltersSettings.computeIfPresent("native_obfuscation", (k, v) -> {
                native_obfuscation.put("filters", v);
                return v;
            });
            // 加入父对象
            config.add("native_obfuscation", native_obfuscation);
        }

        field_initialization:
        {
            if (!fieldInitializationSetting) break field_initialization;

            Map<String, Object> field_initialization = new LinkedHashMap<>();

            subFiltersSettings.computeIfPresent("field_initialization", (k, v) -> {
                field_initialization.put("filters", v);
                return v;
            });
            config.add("field_initialization", field_initialization);
        }

        string_encryption:
        {
            if (!stringEncryptionEnable) break string_encryption;
            Map<String, Object> string_encryption = new LinkedHashMap<>();

            subFiltersSettings.computeIfPresent("string_encryption", (k, v) -> {
                string_encryption.put("filters", v);
                return v;
            });
            config.add("string_encryption", string_encryption);
        }

        renamer:
        {
            if (!renamerEnable) break renamer;
            Map<String, Object> renamer = new LinkedHashMap<>();

            renamer.put("print_mappings", printMappingsSetting);
            renamer.put("print_mappings_file", printMappingsFileSetting);
            renamer.put("prefix_name", prefixNameSetting);
            renamer.put("repackage", repackageSetting);
            renamer.put("repackage_name", repackageNameSetting);
            renamer.put("adapt_resources", adaptResourcesSetting);

            Map<String, Object> mixins = new LinkedHashMap<>();
            mixins.put("mixins_support", mixinsSupportSetting);
            mixins.put("mixins_json", mixinsJsonSetting);
            mixins.put("mixins_ref_json", mixinsRefJsonSetting);

            renamer.put("mixin", mixins);

            subFiltersSettings.computeIfPresent("renamer", (k, v) -> {
                renamer.put("filters", v);
                return v;
            });
            config.add("renamer", renamer);
        }

        member_shuffler:
        {
            if (!memberShufflerEnable) break member_shuffler;
            Map<String, Object> member_shuffler = new LinkedHashMap<>();

            subFiltersSettings.computeIfPresent("member_shuffler", (k, v) -> {
                member_shuffler.put("filters", v);
                return v;
            });
            config.add("member_shuffler", member_shuffler);
        }

        debug_information_remover:
        {
            if (!debugInformationRemoverEnable) break debug_information_remover;
            Map<String, Object> debug_information_remover = new LinkedHashMap<>();

            debug_information_remover.put("remove_signatures", removeSignaturesSetting);
            debug_information_remover.put("remove_source_file", removeSourceFileSetting);
            debug_information_remover.put("remove_inner_class", removeInnerClassSetting);
            debug_information_remover.put("remove_line_number", removeLineNumberSetting);
            debug_information_remover.put("remove_local_variable", removeLocalVariableSetting);
            debug_information_remover.put("remove_kotlin_reference", removeKotlinReferenceSetting);

            subFiltersSettings.computeIfPresent("debug_information_remover", (k, v) -> {
                debug_information_remover.put("filters", v);
                return v;
            });
            config.add("debug_information_remover", debug_information_remover);
        }

        control_flow_obfuscation:
        {
            if (!controlFlowObfuscationEnable) break control_flow_obfuscation;
            Map<String, Object> control_flow_obfuscation = new LinkedHashMap<>();

            subFiltersSettings.computeIfPresent("control_flow_obfuscation", (k, v) -> {
                control_flow_obfuscation.put("filters", v);
                return v;
            });
            config.add("control_flow_obfuscation", control_flow_obfuscation);
        }


        invoke_wrapper_obfuscation:
        {
            if (!invokeWrapperEnable) break invoke_wrapper_obfuscation;
            Map<String, Object> invoke_wrapper_obfuscation = new LinkedHashMap<>();

            invoke_wrapper_obfuscation.put("package_mode", packageModeSetting);
            invoke_wrapper_obfuscation.put("inject_to_other_class", injectToOtherClassSetting);

            subFiltersSettings.computeIfPresent("invoke_wrapper_obfuscation", (k, v) -> {
                invoke_wrapper_obfuscation.put("filters", v);
                return v;
            });
            config.add("invoke_wrapper_obfuscation", invoke_wrapper_obfuscation);
        }

        return config;
    }

    /*======================*/

    public ConfigBuilder setInputJar(File inputJarSetting) {
        this.inputJarSetting = inputJarSetting;
        return this;
    }

    public ConfigBuilder setOutputJar(File outputJarSetting) {
        this.outputJarSetting = outputJarSetting;
        return this;
    }

    public ConfigBuilder setLoaderPackageSetting(String loaderPackageSetting) {
        this.loaderPackageSetting = loaderPackageSetting;
        return this;
    }


    public ConfigBuilder setPrintInstructionsSetting(boolean printInstructionsSetting) {
        this.printInstructionsSetting = printInstructionsSetting;
        return this;
    }

    public ConfigBuilder setNativeObfuscationEnable(boolean nativeObfuscationEnable) {
        this.nativeObfuscationEnable = nativeObfuscationEnable;
        return this;
    }

    public ConfigBuilder setStringEncryptionEnable(boolean stringEncryptionEnable) {
        this.stringEncryptionEnable = stringEncryptionEnable;
        return this;
    }

    public ConfigBuilder addLibrary(String path) {
        this.librariesSettings.add(path);
        return this;
    }

    public ConfigBuilder addLibraries(String... paths) {
        this.librariesSettings.addAll(Arrays.asList(paths));
        return this;
    }

    public ConfigBuilder setCreationDateSetting(String creationDateSetting) {
        this.creationDateSetting = creationDateSetting;
        return this;
    }

    public ConfigBuilder addFilter(String filter) {
        this.filtersSettings.add(filter);
        return this;
    }

    public ConfigBuilder addFilters(String... filters) {
        this.filtersSettings.addAll(Arrays.asList(filters));
        return this;
    }

    public ConfigBuilder addSubFilter(String transformer, String filter) {
        this.subFiltersSettings.compute(transformer, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(filter);
            return v;
        });
        return this;
    }

    public ConfigBuilder addSubFilters(String transformer, String... filters) {
        this.subFiltersSettings.compute(transformer, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.addAll(Arrays.asList(filters));
            return v;
        });
        return this;
    }

    public ConfigBuilder addTarget(String target) {
        this.targetsSettings.add(target);
        return this;
    }

    public ConfigBuilder addTargets(String... targets) {
        this.targetsSettings.addAll(Arrays.asList(targets));
        return this;
    }

    public ConfigBuilder setCppCompilerSetting(String cppCompilerSetting) {
        this.cppCompilerSetting = cppCompilerSetting;
        return this;
    }

    public ConfigBuilder setCppCompilerArgumentsSetting(String cppCompilerArgumentsSetting) {
        this.cppCompilerArgumentsSetting = cppCompilerArgumentsSetting;
        return this;
    }

    public ConfigBuilder setCppCompilerOutputSetting(String cppCompilerOutputSetting) {
        this.cppCompilerOutputSetting = cppCompilerOutputSetting;
        return this;
    }

    public ConfigBuilder setRenamerEnable(boolean renamerEnable) {
        this.renamerEnable = renamerEnable;
        return this;
    }

    public ConfigBuilder setDictionarySetting(String dictionarySetting) {
        this.dictionarySetting = Objects.requireNonNull(dictionarySetting);
        return this;
    }

    public ConfigBuilder setRepackageSetting(boolean repackageSetting) {
        this.repackageSetting = repackageSetting;
        return this;
    }

    public ConfigBuilder setRepackageNameSetting(String repackageNameSetting) {
        this.repackageNameSetting = repackageNameSetting;
        return this;
    }

    public ConfigBuilder setPrintMappingsSetting(boolean printMappingsSetting) {
        this.printMappingsSetting = printMappingsSetting;
        return this;
    }

    public ConfigBuilder addAdaptResources(String adaptResources) {
        this.adaptResourcesSetting.add(adaptResources);
        return this;
    }

    public ConfigBuilder addAdaptResources(String... adaptResources) {
        this.adaptResourcesSetting.addAll(Arrays.asList(adaptResources));
        return this;
    }

    public ConfigBuilder setInputMappingsFileSetting(String inputMappingsFileSetting) {
        this.inputMappingsFileSetting = inputMappingsFileSetting;
        return this;
    }

    public ConfigBuilder setPrintMappingsFileSetting(String printMappingsFileSetting) {
        this.printMappingsFileSetting = printMappingsFileSetting;
        return this;
    }

    public ConfigBuilder setRandomSeedSetting(long randomSeedSetting) {
        this.randomSeedSetting = randomSeedSetting;
        return this;
    }

    public ConfigBuilder setHiddenStackTraceSetting(boolean hiddenStackTraceSetting) {
        this.hiddenStackTraceSetting = hiddenStackTraceSetting;
        return this;
    }

    public ConfigBuilder setMemberShufflerEnable(boolean memberShufflerEnable) {
        this.memberShufflerEnable = memberShufflerEnable;
        return this;
    }

    public ConfigBuilder setDebugInformationRemoverEnable(boolean debugInformationRemoverEnable) {
        this.debugInformationRemoverEnable = debugInformationRemoverEnable;
        return this;
    }

    public ConfigBuilder setRemoveSignaturesSetting(boolean removeSignaturesSetting) {
        this.removeSignaturesSetting = removeSignaturesSetting;
        return this;
    }

    public ConfigBuilder setRemoveSourceFileSetting(boolean removeSourceFileSetting) {
        this.removeSourceFileSetting = removeSourceFileSetting;
        return this;
    }

    public ConfigBuilder setRemoveInnerClassSetting(boolean removeInnerClassSetting) {
        this.removeInnerClassSetting = removeInnerClassSetting;
        return this;
    }

    public ConfigBuilder setRemoveLineNumberSetting(boolean removeLineNumberSetting) {
        this.removeLineNumberSetting = removeLineNumberSetting;
        return this;
    }

    public ConfigBuilder setRemoveLocalVariableSetting(boolean removeLocalVariableSetting) {
        this.removeLocalVariableSetting = removeLocalVariableSetting;
        return this;
    }

    public ConfigBuilder setPrefixNameSetting(String prefixNameSetting) {
        this.prefixNameSetting = prefixNameSetting;
        return this;
    }

    public ConfigBuilder setControlFlowObfuscationEnable(boolean controlFlowObfuscationEnable) {
        this.controlFlowObfuscationEnable = controlFlowObfuscationEnable;
        return this;
    }

    public ConfigBuilder setInvokeWrapperEnable(boolean invokeWrapperEnable) {
        this.invokeWrapperEnable = invokeWrapperEnable;
        return this;
    }

    public ConfigBuilder setInjectToOtherClassSetting(boolean injectToOtherClassSetting) {
        this.injectToOtherClassSetting = injectToOtherClassSetting;
        return this;
    }

    public ConfigBuilder setPackageModeSetting(String packageModeSetting) {
        this.packageModeSetting = packageModeSetting;
        return this;
    }

    public ConfigBuilder setCppCompilerIsAarch64(boolean cppCompilerIsAarch64) {
        this.cppCompilerIsAarch64 = cppCompilerIsAarch64;
        return this;
    }

    public ConfigBuilder setRemoveKotlinReferenceSetting(boolean removeKotlinReferenceSetting) {
        this.removeKotlinReferenceSetting = removeKotlinReferenceSetting;
        return this;
    }

    public ConfigBuilder setVerificationEnableSetting(boolean verificationEnableSetting) {
        this.verificationEnableSetting = verificationEnableSetting;
        return this;
    }

    public ConfigBuilder setUseInternalUserInterfaceSetting(boolean useInternalUserInterfaceSetting) {
        this.useInternalUserInterfaceSetting = useInternalUserInterfaceSetting;
        return this;
    }

    public ConfigBuilder setVerificationTokenSetting(String verificationTokenSetting) {
        this.verificationTokenSetting = verificationTokenSetting;
        return this;
    }

    public ConfigBuilder setVerificationServerSetting(String verificationServerSetting) {
        this.verificationServerSetting = verificationServerSetting;
        return this;
    }

    public ConfigBuilder setVerificationSoftwareIdSetting(String verificationSoftwareIdSetting) {
        this.verificationSoftwareIdSetting = verificationSoftwareIdSetting;
        return this;
    }


    public ConfigBuilder setVerificationUserIdSetting(String verificationUserIdSetting) {
        this.verificationUserIdSetting = verificationUserIdSetting;
        return this;
    }

    public ConfigBuilder setNullSafetySetting(boolean nullSafetySetting) {
        this.nullSafetySetting = nullSafetySetting;
        return this;
    }

    public ConfigBuilder setPrintClassesAsDirectorySetting(boolean printClassesAsDirectorySetting) {
        this.printClassesAsDirectorySetting = printClassesAsDirectorySetting;
        return this;
    }

    public ConfigBuilder setGeneratePhantomClassesSetting(boolean generatePhantomClassesSetting) {
        this.generatePhantomClassesSetting = generatePhantomClassesSetting;
        return this;
    }

    public ConfigBuilder setVerificationKeepAliveSetting(boolean verificationKeepAliveSetting) {
        this.verificationKeepAliveSetting = verificationKeepAliveSetting;
        return this;
    }

    public ConfigBuilder setFieldInitializationSetting(boolean fieldInitializationSetting) {
        this.fieldInitializationSetting = fieldInitializationSetting;
        return this;
    }

    public ConfigBuilder setDebugSetting(boolean debugSetting) {
        this.debugSetting = debugSetting;
        return this;
    }

    public ConfigBuilder setMinimumGeneratedNameLengthSetting(long minimumGeneratedNameLengthSetting) {
        this.minimumGeneratedNameLengthSetting = minimumGeneratedNameLengthSetting;
        return this;
    }

    public ConfigBuilder setMixinsJsonSetting(String mixinsJsonSetting) {
        this.mixinsJsonSetting = mixinsJsonSetting;
        return this;
    }

    public ConfigBuilder setMixinsRefJsonSetting(String mixinsRefJsonSetting) {
        this.mixinsRefJsonSetting = mixinsRefJsonSetting;
        return this;
    }

    public ConfigBuilder setMixinsSupportSetting(boolean mixinsSupportSetting) {
        this.mixinsSupportSetting = mixinsSupportSetting;
        return this;
    }
}