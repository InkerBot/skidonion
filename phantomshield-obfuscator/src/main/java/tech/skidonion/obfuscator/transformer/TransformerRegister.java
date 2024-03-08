package tech.skidonion.obfuscator.transformer;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.transformer.impl.*;
import tech.skidonion.obfuscator.value.Value;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;

public class TransformerRegister {
    private final Map<String, Transformer> instances = new LinkedHashMap<>();

    public TransformerRegister() {
        this.register(new DebugInformationRemover("debug_information_remover"));
        this.register(new MemberShuffler("member_shuffler"));
        this.register(new Renamer("renamer"));
        this.register(new StringEncryption("string_encryption"));
        this.register(new InvokeWrapperObfuscation("invoke_wrapper_obfuscation"));
        this.register(new ControlFlowObfuscation("control_flow_obfuscation"));
        this.register(new NativeObfuscation("native_obfuscation"));
    }

    public void register(Transformer instance) {
        this.instances.put(instance.getName(), instance);
    }

    public Transformer get(String name) {
        return instances.get(name);
    }

    @SuppressWarnings("unchecked")
    public void parseConfig(Config config) {
        Filter filter = null;
        List<String> filters = config.getList("filters");
        if (filters != null) {
            filter = new Filter();
            for (String element : filters) {
                filter.accept(element);
            }
        }

        for (Entry<String, Transformer> entry : instances.entrySet()) {
            String name = entry.getKey();
            Transformer instance = entry.getValue();
            Map<String, Object> settings = config.getMap(name);
            if (settings != null) {
                instance.setEnabled(true);
                for (Value<?> value : instance.getSettings()) {
                    Object element = settings.get(value.getName());
                    if (element != null) {
                        value.parseConfig(element);
                    }
                }


                List<String> subfilters = (List<String>) settings.get("filters");
                if (subfilters != null) {
                    Filter subfilter = new Filter(filter);
                    for (String element : subfilters) {
                        subfilter.accept(element);
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
                ERROR("Occurred an error while preprocessing.", e);
            }
        });

        instances.values().stream().filter(instance -> Objects.nonNull(instance) && instance.isEnabled()).forEach(instance -> {
            PhantomShield.INFO("-----------------------");
            PhantomShield.INFO("Transformer: {}", instance.getName());
            long current = System.currentTimeMillis();
            try {
                instance.transform();
            } catch (Exception e) {
                ERROR("Occurred an error while preprocessing.", e);
            }
            PhantomShield.INFO("Finished running {} transformer. [{}ms]", instance.getName(), (System.currentTimeMillis() - current));
            PhantomShield.INFO("-----------------------");
        });
    }
}
