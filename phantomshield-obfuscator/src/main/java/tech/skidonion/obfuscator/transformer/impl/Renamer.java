package tech.skidonion.obfuscator.transformer.impl;

import com.google.gson.*;
import org.objectweb.asm.Type;
import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;
import tech.skidonion.obfuscator.filter.AntPathMatcher;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.renamer.Mapper;
import tech.skidonion.obfuscator.value.impls.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.*;

@LoadAfterLogin(value = "基础用户组", priority = 1)
public class Renamer extends Transformer {
    private final static AntPathMatcher matcher = new AntPathMatcher();
    private final static Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final BooleanValue print_mappings = new BooleanValue("print_mappings", false);
    private final StringValue print_mappings_file = new StringValue("print_mappings_file", "mappings.txt");
    //    TODO: encrypted number line number for stack trace
//    private final BooleanValue encrypted_number_line = new BooleanValue("encrypted_number_line", false);
    public final StringValue prefix_name = new StringValue("prefix_name", "");
    private final BooleanValue repackage = new BooleanValue("repackage", false);
    public final ClassPackageValue repackage_name = new ClassPackageValue("repackage_name", "skidonion/??????");
    private final StringArrayValue adapt_resources = new StringArrayValue("adapt_resources");

    // --- mixin support ---
    public final BooleanValue mixin_support = new BooleanValue("mixin_support", false);
    private final StringValue mixins_json = new StringValue("mixins_json", "mixins.json");
    private final StringValue mixins_ref_json = new StringValue("mixins_ref_json", "mixins.ref.json");
    private final SubValue mixin = new SubValue("mixin", mixin_support, mixins_json, mixins_ref_json);

    private Mapper mapper;


    public Renamer(String name) {
        super(name);
        addSettings(print_mappings, print_mappings_file/*, encrypted_number_line*/, prefix_name, repackage, repackage_name, adapt_resources, mixin);
    }


    @Override
    public void transform() throws InterruptedException {
    }

    @Override
    public void postprocess() throws Exception {
    }

    @Override
    public void preprocess() throws Exception {
        mapper = new Mapper(obfuscator, getClassWrappers(), Collections.emptyList(), this);
        mapper.setPrefixName(prefix_name.getValue());
        mapper.setRepackage(mixin_support.isEnable() ? false : repackage.isEnable());
        mapper.setRepakageName(repackage_name.getValue());

        if (obfuscator.getConfig().has("input_mappings_file")) {
            INFO(TRANSLATION("phantom-shield-x.renamer.input"));
            long current = System.currentTimeMillis();
            mapper.resolveInputMapping(new File(obfuscator.getConfig().getString("input_mappings_file")));
            INFO(TRANSLATION("phantom-shield-x.renamer.resolved"), System.currentTimeMillis() - current);
        }


        INFO(TRANSLATION("phantom-shield-x.renamer.generate"));
        long current = System.currentTimeMillis();
        mapper.generateMappings();
        INFO(TRANSLATION("phantom-shield-x.renamer.finish"), System.currentTimeMillis() - current);


        INFO(TRANSLATION("phantom-shield-x.renamer.apply"));
        current = System.currentTimeMillis();
        Optional<String> opt = Wrapper.getCloudConstant(271423823, 0);

        if (!opt.isPresent() || (Integer.parseInt(opt.get()) ^ 1825605542) != 1789160537) {
            Thread.sleep(10000L);
        }

        mapper.apply();
        INFO(TRANSLATION("phantom-shield-x.renamer.mapped"), mapper.getMappings().size(), System.currentTimeMillis() - current);


        // Now we gotta fix those resources because we probably screwed up random files.
        INFO(TRANSLATION("phantom-shield-x.renamer.attempt"));
        current = System.currentTimeMillis();
        AtomicInteger fixed = new AtomicInteger();
        getResources().forEach((name, byteArray) -> adapt_resources.getValue().forEach(s -> {

            if (matcher.match(s, name)) {
                String stringVer = new String(byteArray, StandardCharsets.UTF_8);

                for (String mapping : mapper.getClassMappings().keySet()) {
                    String original = mapping.replace("/", ".");
                    if (stringVer.contains(original)) {
                        // Regex that ensures that class names that match words in the manifest don't break the
                        // manifest.
                        // Example: name == Main
                        if ("META-INF/MANIFEST.MF".equals(name) // Manifest
                                || "plugin.yml".equals(name) // Spigot plugin
                                || "bungee.yml".equals(name)) // Bungeecord plugin
                            stringVer = stringVer.replaceAll("(?<=[: ])" + original, mapper.getClassMappings().get(mapping).replace("/", "."));
                        else
                            stringVer = stringVer.replace(original, mapper.getClassMappings().get(mapping).replace("/", "."));
                    }
                }

                getResources().put(name, stringVer.getBytes(StandardCharsets.UTF_8));
                fixed.incrementAndGet();
            }
        }));
        INFO(TRANSLATION("phantom-shield-x.renamer.mapped2"), fixed.get(), System.currentTimeMillis() - current);

        if (mixin_support.isEnable()) {
            INFO(TRANSLATION("phantom-shield-x.renamer.mixin-resource"));
            current = System.currentTimeMillis();
            byte[] bytes;
            bytes = getResources().get(mixins_json.getValue());
            process:
            if (bytes != null) {
                JsonObject mixinJson = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();

                if (mixinJson.has("package")) {
                    String pkg = mixinJson.get("package").getAsString();
                    String npkg = pkg.replace('.', '/') + "/";
                    if (mapper.getPackageMappings().containsKey(npkg)) {
                        npkg = mapper.getPackageMappings().get(npkg).replace('/', '.');
                    } else {
                        WARN(TRANSLATION("phantom-shield-x.renamer.mixin-package-not-found"));
                    }
                    mixinJson.addProperty("package", npkg.substring(0, npkg.length() - 1));

                    remapMixinConfig(mixinJson, "mixins", pkg, npkg);
                    remapMixinConfig(mixinJson, "client", pkg, npkg);
                    remapMixinConfig(mixinJson, "server", pkg, npkg);
                } else {
                    ERROR(TRANSLATION("phantom-shield-x.renamer.mixin-miss-package"));
                    break process;
                }

                getResources().put(mixins_json.getValue(), GSON.toJson(mixinJson).getBytes(StandardCharsets.UTF_8));
            } else {
                ERROR(TRANSLATION("phantom-shield-x.renamer.mixin-config-not-found"), mixins_json.getValue());
            }

            bytes = getResources().get(mixins_ref_json.getValue());

            process:
            if (bytes != null) {
                JsonObject refJson = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
                if (refJson.has("mappings")) {
                    JsonObject mappings = refJson.get("mappings").getAsJsonObject();
                    JsonObject mapped = new JsonObject();
                    for (Map.Entry<String, JsonElement> entry : mappings.entrySet()) {
                        String key = entry.getKey();
                        JsonElement value = entry.getValue();
                        if (mapper.getClassMappings().containsKey(key)) {
                            mapped.add(mapper.getClassMappings().get(key), value);
                        } else {
                            WARN(TRANSLATION("phantom-shield-x.renamer.mixin-not-found"), key);
                        }
                    }
                    refJson.add("mappings", mapped);

                } else {
                    ERROR(TRANSLATION("phantom-shield-x.renamer.mixin-miss-mappings"));
                    break process;
                }

                getResources().put(mixins_ref_json.getValue(), GSON.toJson(refJson).getBytes(StandardCharsets.UTF_8));

            } else {
                ERROR(TRANSLATION("phantom-shield-x.renamer.mixin-config-not-found"), mixins_ref_json.getValue());
            }

            INFO(TRANSLATION("phantom-shield-x.renamer.mixin-resource-finish"), System.currentTimeMillis() - current);
        }


        if (print_mappings.isEnable()) {
            current = System.currentTimeMillis();
            INFO(TRANSLATION("phantom-shield-x.renamer.print"));
            File file = new File(print_mappings_file.getValue());
            mapper.printMappings(file);
            INFO(TRANSLATION("phantom-shield-x.renamer.finished2"), file.getAbsolutePath(), System.currentTimeMillis() - current);
        }
    }

    private void remapMixinConfig(JsonObject mixinJson, String element, String pkg, String npkg) {
        int subLength = npkg.length();
        if (!mixinJson.has(element))
            return;
        JsonArray array = mixinJson.get(element).getAsJsonArray();

        for (int i = 0; array.size() > i; i++) {
            String clz = array.get(i).getAsString();
            clz = (pkg + "." + clz).replace('.', '/');
            if (mapper.getClassMappings().containsKey(clz)) {
                array.set(i, new JsonPrimitive(mapper.getClassMappings().get(clz).substring(subLength).replace('/', '.')));
            } else {
                WARN(TRANSLATION("phantom-shield-x.renamer.mixin-not-found"), clz);
            }
        }
    }

    @Override
    public String annotation() {
        return Type.getDescriptor(tech.skidonion.obfuscator.annotations.Renamer.class);
    }


}
