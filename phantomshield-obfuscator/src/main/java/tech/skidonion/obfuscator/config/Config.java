package tech.skidonion.obfuscator.config;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import java.io.*;

public class Config {
    private JsonObject config;

    Config() {
        this.config = new JsonObject();
    }

    Config(File file) throws IOException {
        this.parse(file);
    }

    public <V> void add(String key, V value) {
        if (value instanceof Number) {
            Number v = ((Number) value);
            this.config.addProperty(key, v);
        } else if (value instanceof Boolean) {
            Boolean v = ((Boolean) value);
            this.config.addProperty(key, v);
        } else if (value instanceof String) {
            String v = ((String) value);
            this.config.addProperty(key, v);
        } else if (value instanceof Character) {
            Character v = ((Character) value);
            this.config.addProperty(key, v);
        } else if (value instanceof JsonElement) {
            JsonElement v = ((JsonElement) value);
            this.config.add(key, v);
        } else {
            throw new RuntimeException("Invalid Config Value");
        }
    }

    public JsonPrimitive getAsJsonPrimitive(String memberName) {
        return config.getAsJsonPrimitive(memberName);
    }

    public JsonArray getAsJsonArray(String memberName) {
        return config.getAsJsonArray(memberName);
    }

    public JsonObject getAsJsonObject(String memberName) {
        return config.getAsJsonObject(memberName);
    }

    public boolean has(String memberName) {
        return config.has(memberName);
    }

    public void save(File configFile) throws IOException {
        Writer writer = new FileWriter(configFile);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(this.config, writer);
        writer.close();
    }

    public void parse(File configFile) throws IOException {
        Reader reader = new FileReader(configFile);
        this.config = (JsonObject) JsonParser.parseReader(reader);
    }

    public static Config readConfig(File file) throws IOException {
        return new Config(file);
    }
}
