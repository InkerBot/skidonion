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
    private final List<String> libraries = new ArrayList<>();
    private final List<String> filters = new ArrayList<>();

    // sub filters
    private final Map<String, List<String>> sub_filters = new HashMap<>();

    // ======= transformers settings =======

    // string obfuscation
    private boolean stringObfuscation = false;

    // native obfuscation
    private boolean nativeObfuscation = false;
    private boolean printInstructions = false;

    private String modeInvokeDynamicNativeConverter = "compatibility";

    public Config build() {
        Config config = new Config();
        config.add("input", Objects.requireNonNull(inputJar, "input is null").getAbsoluteFile().toString());
        config.add("output", Objects.requireNonNull(outputJar, "output is null").getAbsoluteFile().toString());

        if (creationDate != null) {
            config.add("creation_date", creationDate);
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
            native_obfuscation.addProperty("invokedynamic_mode", modeInvokeDynamicNativeConverter);
            native_obfuscation.addProperty("print_instructions", printInstructions);

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

        string_obfuscation:
        {
            if (!stringObfuscation) break string_obfuscation;
            JsonObject string_obfuscation = new JsonObject();

            sub_filters.computeIfPresent("string_obfuscation", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                string_obfuscation.add("filters", array);
                return v;
            });
            config.add("string_obfuscation", string_obfuscation);
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

    public ConfigBuilder setStringObfuscation(boolean stringObfuscation) {
        this.stringObfuscation = stringObfuscation;
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

}