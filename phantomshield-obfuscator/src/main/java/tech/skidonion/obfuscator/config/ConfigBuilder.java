package tech.skidonion.obfuscator.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.*;

public class ConfigBuilder {

    // attributions
    private File inputJar;
    private File outputJar;
    private String creationDate;
    private String cppCompiler;
    private String cppCompilerArguments;
    private String cppCompilerOutput;
    private long randomSeed;
    private String dictionary = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final List<String> targets = new ArrayList<>();
    private final List<String> libraries = new ArrayList<>();
    private final List<String> filters = new ArrayList<>();

    // sub filters
    private final Map<String, List<String>> sub_filters = new HashMap<>();

    // ======= transformers settings =======

    // string encryption
    private boolean stringEncryption = false;

    // native obfuscation
    private boolean nativeObfuscation = false;
    private String loaderPackage = "skidonion/??????";
    private boolean printInstructions = false;
    private String modeInvokeDynamicNativeConverter = "compatibility";
    private boolean hiddenStackTrace = true;

    // renamer
    private boolean renamer = false;
    private boolean repackage = false;
    private String repackageName = "skidonion/??????";
    private boolean importExistingMappings = false;
    private String inputMappingsFile = "mappings.txt";
    private boolean printMappings = false;
    private String printMappingsFile = "mappings.txt";
    private List<String> adaptResources = new ArrayList<>();

    // member shuffler
    private boolean memberShuffler = false;

    public final Config build() {
        Config config = new Config();
        config.add("input", Objects.requireNonNull(inputJar, "input is null").getAbsoluteFile().toString());
        config.add("output", Objects.requireNonNull(outputJar, "output is null").getAbsoluteFile().toString());
        config.add("dictionary", dictionary);

        if (creationDate != null) {
            config.add("creation_date", creationDate);
        }

        if (randomSeed != 0) {
            config.add("random_seed", randomSeed);
        }

        if (cppCompiler != null) {
            config.add("cpp_compiler", cppCompiler);
        }

        if (cppCompilerArguments != null) {
            config.add("cpp_compiler_arguments", cppCompilerArguments);
        }

        if (cppCompilerOutput != null) {
            config.add("cpp_compiler_output", cppCompilerOutput);
        }


        if (!targets.isEmpty()) {
            JsonArray array = new JsonArray();
            targets.forEach(array::add);
            config.add("targets", array);
        }

        if (!libraries.isEmpty()) {
            JsonArray array = new JsonArray();
            libraries.forEach(array::add);
            config.add("libraries", array);
        }

        if (!filters.isEmpty()) {
            JsonArray array = new JsonArray();
            filters.forEach(array::add);
            config.add("filters", array);
        }

        // 处理变压器的方法
        native_obfuscation:
        {
            // 如果不开启则直接跳过代码块
            if (!nativeObfuscation) break native_obfuscation;
            // 生成一个子json对象
            JsonObject native_obfuscation = new JsonObject();

            // 添加 settings
            native_obfuscation.addProperty("loader_package", loaderPackage);
            native_obfuscation.addProperty("print_instructions", printInstructions);
            native_obfuscation.addProperty("invokedynamic_mode", modeInvokeDynamicNativeConverter);
            native_obfuscation.addProperty("hidden_stack_trace", hiddenStackTrace);

            // 添加 过滤器
            sub_filters.computeIfPresent("native_obfuscation", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                native_obfuscation.add("filters", array);
                return v;
            });
            // 加入父对象
            config.add("native_obfuscation", native_obfuscation);
        }

        string_encryption:
        {
            if (!stringEncryption) break string_encryption;
            JsonObject string_encryption = new JsonObject();

            sub_filters.computeIfPresent("string_encryption", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                string_encryption.add("filters", array);
                return v;
            });
            config.add("string_encryption", string_encryption);
        }

        renamer:
        {
            if (!renamer) break renamer;
            JsonObject renamer = new JsonObject();

            renamer.addProperty("import_existing_mappings", importExistingMappings);
            renamer.addProperty("input_mappings_file", inputMappingsFile);
            renamer.addProperty("print_mappings", printMappings);
            renamer.addProperty("print_mappings_file", printMappingsFile);
            renamer.addProperty("repackage", repackage);
            renamer.addProperty("repackage_name", repackageName);
            renamer.add("adapt_resources", adaptResources.stream().collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

            sub_filters.computeIfPresent("renamer", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                renamer.add("filters", array);
                return v;
            });
            config.add("renamer", renamer);
        }

        member_shuffler:
        {
            if (!memberShuffler) break member_shuffler;
            JsonObject member_shuffler = new JsonObject();

            sub_filters.computeIfPresent("member_shuffler", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                member_shuffler.add("filters", array);
                return v;
            });
            config.add("member_shuffler", member_shuffler);
        }

        return config;
    }

    public ConfigBuilder setInputJar(File inputJar) {
        this.inputJar = inputJar;
        return this;
    }

    public ConfigBuilder setOutputJar(File outputJar) {
        this.outputJar = outputJar;
        return this;
    }

    public ConfigBuilder setLoaderPackage(String loaderPackage) {
        this.loaderPackage = loaderPackage;
        return this;
    }

    public ConfigBuilder setModeInvokeDynamicNativeConverter(String modeInvokeDynamicNativeConverter) {
        this.modeInvokeDynamicNativeConverter = modeInvokeDynamicNativeConverter;
        return this;
    }

    public ConfigBuilder setPrintInstructions(boolean printInstructions) {
        this.printInstructions = printInstructions;
        return this;
    }

    public ConfigBuilder setNativeObfuscation(boolean nativeObfuscation) {
        this.nativeObfuscation = nativeObfuscation;
        return this;
    }

    public ConfigBuilder setStringEncryption(boolean stringEncryption) {
        this.stringEncryption = stringEncryption;
        return this;
    }

    public ConfigBuilder addLibrary(String path) {
        this.libraries.add(path);
        return this;
    }

    public ConfigBuilder addLibraries(String... paths) {
        this.libraries.addAll(Arrays.asList(paths));
        return this;
    }

    public ConfigBuilder setCreationDate(String creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public ConfigBuilder addFilter(String filter) {
        this.filters.add(filter);
        return this;
    }

    public ConfigBuilder addFilters(String... filters) {
        this.filters.addAll(Arrays.asList(filters));
        return this;
    }

    public ConfigBuilder addSubFilter(String transformer, String filter) {
        this.sub_filters.compute(transformer, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.add(filter);
            return v;
        });
        return this;
    }

    public ConfigBuilder addSubFilters(String transformer, String... filters) {
        this.sub_filters.compute(transformer, (k, v) -> {
            if (v == null) {
                v = new ArrayList<>();
            }
            v.addAll(Arrays.asList(filters));
            return v;
        });
        return this;
    }

    public ConfigBuilder addTarget(String target) {
        this.targets.add(target);
        return this;
    }

    public ConfigBuilder addTargets(String... targets) {
        this.targets.addAll(Arrays.asList(targets));
        return this;
    }

    public ConfigBuilder setCppCompiler(String cppCompiler) {
        this.cppCompiler = cppCompiler;
        return this;
    }

    public ConfigBuilder setCppCompilerArguments(String cppCompilerArguments) {
        this.cppCompilerArguments = cppCompilerArguments;
        return this;
    }

    public ConfigBuilder setCppCompilerOutput(String cppCompilerOutput) {
        this.cppCompilerOutput = cppCompilerOutput;
        return this;
    }

    public ConfigBuilder setRenamer(boolean renamer) {
        this.renamer = renamer;
        return this;
    }

    public ConfigBuilder setDictionary(String dictionary) {
        this.dictionary = Objects.requireNonNull(dictionary);
        return this;
    }

    public ConfigBuilder setRepackage(boolean repackage) {
        this.repackage = repackage;
        return this;
    }

    public ConfigBuilder setRepackageName(String repackageName) {
        this.repackageName = repackageName;
        return this;
    }

    public ConfigBuilder setPrintMappings(boolean printMappings) {
        this.printMappings = printMappings;
        return this;
    }

    public ConfigBuilder addAdaptResources(String adaptResources) {
        this.adaptResources.add(adaptResources);
        return this;
    }

    public ConfigBuilder addAdaptResources(String... adaptResources) {
        this.adaptResources.addAll(Arrays.asList(adaptResources));
        return this;
    }

    public ConfigBuilder setImportExistingMappings(boolean importExistingMappings) {
        this.importExistingMappings = importExistingMappings;
        return this;
    }

    public ConfigBuilder setInputMappingsFile(String inputMappingsFile) {
        this.inputMappingsFile = inputMappingsFile;
        return this;
    }

    public ConfigBuilder setPrintMappingsFile(String printMappingsFile) {
        this.printMappingsFile = printMappingsFile;
        return this;
    }

    public ConfigBuilder setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
        return this;
    }

    public ConfigBuilder setHiddenStackTrace(boolean hiddenStackTrace) {
        this.hiddenStackTrace = hiddenStackTrace;
        return this;
    }
    public ConfigBuilder setMemberShuffler(boolean memberShuffler) {
        this.memberShuffler = memberShuffler;
        return this;
    }
}