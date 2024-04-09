package tech.skidonion.obfuscator.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private Map<String, Object> config;

    Config() {
        this.config = new LinkedHashMap<>();
    }

    Config(File file) throws FileNotFoundException {
        this.parse(file);
    }

    public <V> void add(String key, V value) {
        this.config.put(key, value);
    }

    public Object get(String key) {
        return this.config.get(key);
    }

    public String getString(String key) {
        return (String) this.config.get(key);
    }

    public int getInteger(String key) {
        return (int) this.config.get(key);
    }

    public long getLong(String key) {
        return (long) this.config.get(key);
    }

    public Number getNumber(String key) {
        return (Number) this.config.get(key);
    }


    public boolean getBoolean(String key) {
        return (boolean) this.config.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends List<?>> T getList(String key) {
        return (T) this.config.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T extends Map<String, Object>> T getMap(String key) {
        if (this.config.get(key) == null) return null;
        return (T) this.config.get(key);
    }

    public boolean has(String key) {
        return this.config.containsKey(key);
    }

    public void save(File file) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);
        FileWriter writer = new FileWriter(file);

        yaml.dump(this.config, writer);
        writer.close();
    }

    public void parse(File file) throws FileNotFoundException {
        Yaml yaml = new Yaml();
        this.config = yaml.load(new FileReader(file));
    }

    public static Config readConfig(File file) throws FileNotFoundException {
        return new Config(file);
    }
}
