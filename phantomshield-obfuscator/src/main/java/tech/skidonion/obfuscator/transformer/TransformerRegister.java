package tech.skidonion.obfuscator.transformer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.transformer.impl.MemberShuffler;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.Renamer;
import tech.skidonion.obfuscator.transformer.impl.StringEncryption;
import tech.skidonion.obfuscator.value.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

public class TransformerRegister {
    private final Map<String, Transformer> instances = new LinkedHashMap<>();

    public TransformerRegister() {
        this.register(new MemberShuffler("member_shuffler"));
        this.register(new Renamer("renamer"));
        this.register(new StringEncryption("string_encryption"));
        this.register(new NativeObfuscation("native_obfuscation"));
    }

    public void register(Transformer instance) {
        this.instances.put(instance.getName(), instance);
    }

    public Transformer get(String name) {
        return instances.get(name);
    }

    public void parseConfig(Config config) {
        Filter filter = null;
        JsonArray filters = config.getAsJsonArray("filters");
        if (filters != null) {
            filter = new Filter();
            for (JsonElement element : filters) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    filter.accept(primitive.getAsString());
                }
            }
        }

        for (Entry<String, Transformer> entry : instances.entrySet()) {
            String name = entry.getKey();
            Transformer instance = entry.getValue();
            JsonObject settings = config.getAsJsonObject(name);
            if (settings != null) {
                instance.setEnabled(true);
                for (Value<?> value : instance.getSettings()) {
                    JsonElement element = settings.get(value.getName());
                    if (element != null) {
                        value.setValue(element);
                    }
                }


                JsonArray subfilters = settings.getAsJsonArray("filters");
                if (subfilters != null) {
                    Filter subfilter = new Filter(filter);
                    for (JsonElement element : subfilters) {
                        JsonPrimitive primitive = element.getAsJsonPrimitive();
                        if (primitive.isString()) {
                            subfilter.accept(primitive.getAsString());
                        }
                    }
                    instance.setFilter(subfilter);
                } else {
                    instance.setFilter(filter);
                }
            }
        }
    }

    public void process(PhantomShield obfuscator) {
        instances.values().stream().filter(instance -> Objects.nonNull(instance) && instance.isEnabled()).forEach(instance -> {
            instance.init(obfuscator);
            try {
                instance.preprocess();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        instances.values().stream().filter(instance -> Objects.nonNull(instance) && instance.isEnabled()).forEach(instance -> {
            PhantomShield.INFO("-----------------------");
            PhantomShield.INFO("Transformer: {}", instance.getName());
            long current = System.currentTimeMillis();
            try {
                instance.transform();
            } catch (Exception e) {
                e.printStackTrace();
            }
            PhantomShield.INFO("Finished running {} transformer. [{}ms]", instance.getName(), (System.currentTimeMillis() - current));
            PhantomShield.INFO("-----------------------");
        });
    }
}
