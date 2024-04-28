package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Type;
import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.renamer.Mapper;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ClassPackageValue;
import tech.skidonion.obfuscator.value.impls.StringArrayValue;
import tech.skidonion.obfuscator.value.impls.StringValue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

@LoadAfterLogin(value = "基础用户组", priority = 1)
public class Renamer extends Transformer {

    private final BooleanValue print_mappings = new BooleanValue("print_mappings", false);
    private final StringValue print_mappings_file = new StringValue("print_mappings_file", "mappings.txt");
    //    TODO: encrypted number line number for stack trace
//    private final BooleanValue encrypted_number_line = new BooleanValue("encrypted_number_line", false);
    public final StringValue prefix_name = new StringValue("prefix_name", "");
    private final BooleanValue repackage = new BooleanValue("repackage", false);
    public final ClassPackageValue repackage_name = new ClassPackageValue("repackage_name", "skidonion/??????");
    private final StringArrayValue adapt_resources = new StringArrayValue("adapt_resources");
    private Mapper mapper;


    public Renamer(String name) {
        super(name);
        addSettings(print_mappings, print_mappings_file/*, encrypted_number_line*/, prefix_name, repackage, repackage_name, adapt_resources);
    }


    @Override
    public void transform() throws InterruptedException {
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
            Pattern pattern = Pattern.compile(s);

            if (pattern.matcher(name).matches()) {
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

        if (print_mappings.isEnable()) {
            current = System.currentTimeMillis();
            INFO(TRANSLATION("phantom-shield-x.renamer.print"));
            File file = new File(print_mappings_file.getValue());
            mapper.printMappings(file);
            INFO(TRANSLATION("phantom-shield-x.renamer.finished2"), file.getAbsolutePath(), System.currentTimeMillis() - current);
        }
    }

    @Override
    public void preprocess() throws Exception {
        mapper = new Mapper(obfuscator, getClassWrappers(), Collections.emptyList(), this);
        mapper.setPrefixName(prefix_name.getValue());
        mapper.setRepackage(repackage.isEnable());
        mapper.setRepakageName(repackage_name.getValue());
    }

    @Override
    public String annotation() {
        return Type.getDescriptor(tech.skidonion.obfuscator.annotations.Renamer.class);
    }


}
