package tech.skidonion.obfuscator.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.*;

public class ConfigBuilder {
    private File inputJar;
    private File outputJar;
    private String creationDate;
    private final List<String> libraries = new ArrayList<>();
    private final List<String> filters = new ArrayList<>();
    private final Map<String, List<String>> sub_filters = new HashMap<>();
    private String modeInvokeDynamicNativeConverter = "compatibility";

    public Config build() {
        if (inputJar == null || outputJar == null) {
            throw new IllegalStateException("Input jar and output directory must be set");
        }
        Config config = new Config();
        config.add("input", inputJar.getAbsoluteFile().toString());
        config.add("output", outputJar.getAbsoluteFile().toString());

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

        native_obfuscation:
        {
            JsonObject native_obfuscation = new JsonObject();

            native_obfuscation.addProperty("invokedynamic_mode", modeInvokeDynamicNativeConverter);

            sub_filters.computeIfPresent("native_obfuscation", (k, v) -> {
                JsonArray array = new JsonArray();
                v.forEach(array::add);
                native_obfuscation.add("filters", array);
                return v;
            });
            config.add("native_obfuscation", native_obfuscation);
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