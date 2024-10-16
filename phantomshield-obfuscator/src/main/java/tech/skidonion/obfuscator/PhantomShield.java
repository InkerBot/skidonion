package tech.skidonion.obfuscator;

import org.clyze.jphantom.ClassMembers;
import org.clyze.jphantom.JPhantom;
import org.clyze.jphantom.Phantoms;
import org.clyze.jphantom.access.ClassAccessStateMachine;
import org.clyze.jphantom.access.FieldAccessStateMachine;
import org.clyze.jphantom.access.MethodAccessStateMachine;
import org.clyze.jphantom.adapters.ClassPhantomExtractor;
import org.clyze.jphantom.hier.ClassHierarchy;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;
import tech.skidonion.obfuscator.asm.ClassTree;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.cpp.CompilerUpdater;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.DictionaryFactory;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.inline.Inline;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.TransformerRegister;
import tech.skidonion.obfuscator.transformer.addon.Watermarking;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.IOUtils;
import tech.skidonion.obfuscator.utils.JPhantomUtils;
import tech.skidonion.obfuscator.utils.commons.UTF8Control;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarFile;
import java.util.zip.*;

@NativeObfuscation
@LoadAfterLogin(value = "基础用户组", priority = 0)
public class PhantomShield {
    public static final boolean DEBUG = false;
    public static ResourceBundle BUNDLE;
    public static final String VERSION = "v0.2.1.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(PhantomShield.class);
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public final Map<String, ClassWrapper> classes = new LinkedHashMap<>();
    public final Map<String, ClassWrapper> softExclusions = new HashMap<>();
    public final Map<String, ClassWrapper> classpath = new HashMap<>();
    public final Map<String, byte[]> resources = new HashMap<>();
    public final Map<String, Dictionary> classesDictionaries = new HashMap<>();
    public final Map<String, Dictionary> packageDictionaries = new HashMap<>();
    private final Map<String, ClassTree> hierarchy = new HashMap<>();
    private final TransformerRegister register = new TransformerRegister();
    private final Config config;
    private long seed;
    private boolean printClassesAsDirectory;
    private Dictionary dictionary;
    private CppCompiler compiler;

    public PhantomShield(File file) throws IOException {
        this(Config.readConfig(file));
        if (!config.has("input"))
            throw new RuntimeException(BUNDLE.getString("phantom-shield-x.instance.no-input"));
        if (!config.has("output"))
            throw new RuntimeException(BUNDLE.getString("phantom-shield-x.instance.no-output"));
    }

    public PhantomShield(Config config) {
        if (BUNDLE == null) {
            BUNDLE = ResourceBundle.getBundle("i18n.lang", new UTF8Control());
        }
        this.config = config;
    }

    @NativeObfuscation()
    @SuppressWarnings("unchecked")
    public void process() {
        INFO("Java Home: {}", System.getProperty("java.home"));
        INFO("Phantom Shield X {}\n{}\n{}", VERSION, "Copyright 2024 fl0wowp4rty", "All rights reserved");

        if (config.has("__debug") && config.getBoolean("__debug")) {
            WARN(BUNDLE.getString("phantom-shield-x.debug"));
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
                WARN(BUNDLE.getString("phantom-shield-x.debug"));
            }
        }

        if (config.has("print_classes_as_directory")) {
            printClassesAsDirectory = config.getBoolean("print_classes_as_directory");
        }

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
        Optional<LocalDateTime> basicRole = Wrapper.getExpiredDate("基础用户组");
        Optional<LocalDateTime> verificationRole = Wrapper.getExpiredDate("授权验证用户组");
        if (basicRole.isPresent()) {
            INFO(BUNDLE.getString("phantom-shield-x.instance.basic-role"), format.format(basicRole.get()));
        } else {
            ERROR(BUNDLE.getString("phantom-shield-x.instance.unregistered"));
            return;
        }

        if (verificationRole.isPresent()) {
            INFO(BUNDLE.getString("phantom-shield-x.instance.verification-role"), format.format(verificationRole.get()));
        }


        if (config.has("dictionary")) {
            Object value = config.get("dictionary");
            if (value instanceof List) {
                List<String> list = ((List<String>) value);
                if (list.size() < 2)
                    throw new RuntimeException(BUNDLE.getString("phantom-shield-x.instance.dictionary-length"));
                dictionary = DictionaryFactory.getCustom(((List<String>) value));
            } else if (value instanceof String) {
                String string = (String) value;
                if (string.length() < 2)
                    throw new RuntimeException(BUNDLE.getString("phantom-shield-x.instance.dictionary-length"));
                dictionary = DictionaryFactory.get(string);
            }
        } else {
            dictionary = DictionaryFactory.get("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        }

        if (config.has("minimum_generated_name_length")) {
            int length = config.getNumber("minimum_generated_name_length").intValue();
            if (length > 1) {
                int n = 0;
                for (int i = 1; i < length; i++) {
                    n += (int) Math.pow(dictionary.size(), i);
                }
                dictionary.getOffset().set(n);
            }
        }

        if (config.has("cpp_compiler")) {
            compiler = new CppCompiler(config.getString("cpp_compiler"));
        } else {
            compiler = new CppCompiler(CompilerUpdater.DEFAULT_COMPILER);
        }
        compiler.init(this);
        if (config.has("cpp_compiler_arguments"))
            compiler.setExtraCommandLine(config.getString("cpp_compiler_arguments"));
        if (config.has("cpp_compiler_output"))
            compiler.setDefaultOutput(config.getString("cpp_compiler_output"));
        if (config.has("targets")) {
            List<String> targets = config.getList("targets");
            targets.forEach(element -> compiler.addTarget(element));
        }
        if (config.has("cpp_compiler_is_aarch64")) {
            compiler.setAarch64(config.getBoolean("cpp_compiler_is_aarch64"));
        }
//        if (config.has("legacy_compile_mode")) {
//            compiler.setLegacyCompileMode(config.getBoolean("legacy_compile_mode"));
//        }

        if (Inline._advanced_checkProtection(0x65328212) == 0x65328212) {
            loadClassPath();
            loadInput();
        }

        INFO(BUNDLE.getString("phantom-shield-x.instance.attempt-build-inheritance"));
        try {
            this.buildInheritance();
        } catch (Exception e) {
            INFO(BUNDLE.getString("phantom-shield-x.instance.build-inheritance-error"), e);
            return;
        }

        register.parseConfig(config);

        if (!verificationRole.isPresent()) {
            tech.skidonion.obfuscator.transformer.impl.NativeObfuscation nativeObfuscation = (tech.skidonion.obfuscator.transformer.impl.NativeObfuscation) register.get("native_obfuscation");
            if (nativeObfuscation != null && nativeObfuscation.isVerificationEnable()) {
                ERROR(BUNDLE.getString("phantom-shield-x.instance.none-verification-role"));
                return;
            }
        }

        register.process(this);

        try {
            Watermarking watermarking = new Watermarking();
            watermarking.init(this);
            watermarking.transform();
        } catch (Exception e) {
            ERROR(BUNDLE.getString("phantom-shield-x.instance.fatal-error"), e);
            return;
        }


        writeOutput();
    }

    @NativeObfuscation(verificationLock = "基础用户组")
    private void writeOutput() {
        File output = new File(config.getString("output"));
        INFO(BUNDLE.getString("phantom-shield-x.instance.output"), output.getAbsolutePath());


        if (output.exists())
            INFO(BUNDLE.getString("phantom-shield-x.instance.exist"), FileUtils.renameExistingFile(output));

        if (Inline._advanced_checkCRCImage(0x86543210) == 0x86543210)
            try {

                DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
                String creationDate = config.getString("creation_date");
                long timestamp = creationDate != null ? formatter.parse(creationDate).getTime() : -1;

                ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output.toPath()));

                new ArrayList<ClassWrapper>() {
                    {
                        addAll(classes.values());
                        addAll(softExclusions.values());
                    }
                }.forEach(classWrapper -> {
                    try {
                        ZipEntry entry = new ZipEntry(classWrapper.getEntryName() + (isPrintClassesAsDirectory() ? "/" : ""));
                        entry.setTime(timestamp);
                        zos.putNextEntry(entry);
                        zos.write(classWrapper.toByteArray());
                        zos.closeEntry();
                    } catch (IOException ioe) {
                        ERROR(BUNDLE.getString("phantom-shield-x.instance.skipping"), classWrapper.getName() + ".class");
                        ERROR("", ioe);
                    }
                });

                resources.forEach((name, bytes) -> {
                    try {
                        if (name.endsWith(".class")) {
                            if (isPrintClassesAsDirectory()) {
                                name += "/";
                            }
                        }
                        ZipEntry entry = new ZipEntry(name);
                        if (name.endsWith(".jar") && name.startsWith("BOOT-INF/lib")) {
                            entry.setMethod(ZipEntry.STORED);
                            entry.setSize(bytes.length);
                            CRC32 crc = new CRC32();
                            crc.update(bytes);
                            entry.setCrc(crc.getValue());
                        }
                        zos.putNextEntry(entry);
                        zos.write(bytes);
                        zos.closeEntry();
                    } catch (IOException ioe) {
                        ERROR(BUNDLE.getString("phantom-shield-x.instance.resource-error"), name);
                        ERROR("", ioe);
                    }
                });
                zos.setComment(String.format("Phantom Shield X %s\n%s", VERSION, "https://skidonion.tech/"));
                zos.close();
            } catch (IOException ioe) {
                ERROR("", ioe);
                throw new RuntimeException();
            } catch (ParseException pe) {
                ERROR("", pe);
                throw new RuntimeException(pe);
            }
    }

    private void loadClassPath(File file) {
        if (file.exists()) {
            if (file.isDirectory()) {
                File[] files = file.listFiles(filter -> {
                    String filename = filter.getName().toLowerCase();
                    return filename.endsWith(".jar") || filename.endsWith(".zip") || filename.endsWith(".jmod");
                });
                if (files != null) {
                    for (File jar : files) {
                        loadClassPath(jar);
                    }
                }
            } else {
                INFO(BUNDLE.getString("phantom-shield-x.instance.load-library"), file.getAbsolutePath());
                try (ZipFile zipFile = new ZipFile(file)) {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    while (entries.hasMoreElements()) {
                        ZipEntry entry = entries.nextElement();

                        if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                            try {
                                ClassWrapper cw = new ClassWrapper(this, new ClassReader(zipFile.getInputStream(entry)), ClassWrapper.ProcessType.LIBRARY);
                                classpath.put(cw.getName(), cw);
                            } catch (Throwable t) {
                                ERROR(BUNDLE.getString("phantom-shield-x.instance.library-error"), entry.getName().replace(".class", ""));
                                ERROR("", t);
                            }
                    }
                } catch (ZipException e) {
                    ERROR(BUNDLE.getString("phantom-shield-x.instance.library-error2"), file.getAbsolutePath());
                    ERROR("", e);
                } catch (IOException e) {
                    ERROR(BUNDLE.getString("phantom-shield-x.instance.library-error3"), file.getAbsolutePath());
                    ERROR("", e);
                }

            }
        } else {
            ERROR(BUNDLE.getString("phantom-shield-x.instance.library-error4"), file.getAbsolutePath());
        }
    }

    private void loadClassPath() {
        List<String> libraries = config.getList("libraries");
        if (libraries != null) {
            for (String library : libraries) {
                File file = new File(library);
                loadClassPath(file);
            }
        }
    }


    private void loadInput() {
        File input = new File(config.getString("input"));
        if (input.exists()) {
            long current = System.currentTimeMillis();
            INFO(BUNDLE.getString("phantom-shield-x.instance.load-input"), input.getAbsolutePath());
            Filter filter = null;
            List<String> filters = config.getList("soft_exclusions");
            if (filters != null) {
                filter = new Filter();
                for (String element : filters) {
                    filter.accept(element);
                }
            }

            Map<String, ClassWrapper> classes = new HashMap<>();

            try (ZipFile zipFile = new ZipFile(input)) {
                Map<String, byte[]> data = new HashMap<>();

                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    InputStream in = zipFile.getInputStream(entry);

                    if (!entry.isDirectory())
                        if (entry.getName().endsWith(".class"))
                            try {
                                byte[] bytes = IOUtils.toByteArray(in);
                                ClassWrapper cw = new ClassWrapper(this, new ClassReader(bytes), false, SKIP_CODE | SKIP_FRAMES | SKIP_DEBUG, null);

                                if (filter == null || !filter.match(cw)) {
                                    cw = new ClassWrapper(this, new ClassReader(bytes), false, SKIP_FRAMES, null);
                                    classes.put(cw.getName(), cw);
                                } else {
                                    cw = new ClassWrapper(this, new ClassReader(bytes), false, 0, 0);
                                    softExclusions.put(cw.getName(), cw);
                                }

                                data.put(cw.getName(), bytes);
                                classpath.put(cw.getName(), cw);

                                String entryName = entry.getName();
                                String wrapperEntryName = cw.getEntryName();
                                if (entryName.endsWith(wrapperEntryName) && !entryName.equals(wrapperEntryName))
                                    cw.setEntryPrefix(entry.getName().substring(0, entryName.length() - wrapperEntryName.length()));
                            } catch (Throwable t) {
                                LOGGER.warn(BUNDLE.getString("phantom-shield-x.instance.input-error"), entry.getName());
                                this.resources.put(entry.getName(), IOUtils.toByteArray(in));
                            }
                        else
                            this.resources.put(entry.getName(), IOUtils.toByteArray(in));
                }

                if (config.has("generate_phantom_classes") && config.getBoolean("generate_phantom_classes")) {

                    Map<Type, org.objectweb.asm.tree.ClassNode> typeMap = new HashMap<>();
                    /*
                     * Set up JPhantom (Recaf matt edition) and read all the classes from our cache.
                     * Use the copied input to generate a class hierarchy. This can be quite slow
                     * at times. I'll need to optimize this process.
                     */
//                    Options.V().setSoftFail(true);
//                    Options.V().setJavaVersion(8);

                    long start = System.currentTimeMillis();
                    INFO(TRANSLATION("phantom-shield-x.jphantom.info1"));

                    ClassHierarchy hierarchy = JPhantomUtils.clsHierarchyFromArchive(new JarFile(input));
                    ClassMembers members = ClassMembers.fromJar(new JarFile(input), hierarchy);

                    data.values().forEach(c -> {
                        ClassReader cr = new ClassReader(c);
                        if (cr.getClassName().contains("$")) {
                            return;
                        }
                        try {
                            cr.accept(new ClassPhantomExtractor(hierarchy, members), 0);
                        } catch (Throwable ignored) {
                        }

                    });


                    // Remove duplicate constraints for faster analysis
                    Set<String> existingConstraints = new HashSet<>();
                    ClassAccessStateMachine.v().getConstraints().removeIf(c -> {
                        boolean isDuplicate = existingConstraints.contains(c.toString());
                        existingConstraints.add(c.toString());
                        return isDuplicate;
                    });

                    JPhantom phantom;
                    // Execute and populate the current resource with generated classes
                    phantom = new JPhantom(typeMap, hierarchy, members);
                    phantom.run();

                    /*
                     * Generate the classes and export them to our phantom content jar
                     * class cache. This will be used as a library during obfuscation.
                     */

                    phantom.getGenerated().forEach((k, v) -> {
                        final byte[] bytes = JPhantomUtils.decorate(v);
                        final ClassReader reader = new ClassReader(bytes);
                        final ClassNode node = new ClassNode();
                        reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

                        if (!classpath.containsKey(node.name)) {
                            classpath.put(node.name, new ClassWrapper(this, node, true, null));
                        }

                    });

                    INFO(TRANSLATION("phantom-shield-x.jphantom.info2"), System.currentTimeMillis() - start);

                    // Cleanup
                    typeMap.clear();
                    Phantoms.refresh();
                    ClassAccessStateMachine.refresh();
                    FieldAccessStateMachine.refresh();
                    MethodAccessStateMachine.refresh();
//                    Files.deleteIfExists(input.toPath());

                }
            } catch (ZipException e) {
                ERROR(BUNDLE.getString("phantom-shield-x.instance.input-error-2"), input.getAbsolutePath());
                ERROR("", e);
                throw new RuntimeException(e);
            } catch (IOException e) {
                ERROR(BUNDLE.getString("phantom-shield-x.instance.input-error-3"), input.getAbsolutePath());
                ERROR("", e);
                throw new RuntimeException(e);
            }

            if (config.has("random_seed")) {
                seed = config.getNumber("random_seed").longValue();
                INFO(BUNDLE.getString("phantom-shield-x.instance.seed"), seed);
            } else {
                seed = ThreadLocalRandom.current().nextLong();
                INFO(BUNDLE.getString("phantom-shield-x.instance.seed"), seed);
            }
            List<String> keys = new ArrayList<>(classes.keySet());
            Collections.shuffle(keys, new Random(seed));
            for (String key : keys) {
                this.classes.put(key, classes.get(key));
            }
            INFO(BUNDLE.getString("phantom-shield-x.instance.load-input-done"), this.classes.size(), System.currentTimeMillis() - current);

        } else {
            ERROR(BUNDLE.getString("phantom-shield-x.instance.input-error-4"), input.getAbsolutePath());
            throw new RuntimeException();
        }
    }

    public Config getConfig() {
        return config;
    }

    public CppCompiler getCompiler() {
        return compiler;
    }

    /**
     * Equivalent to the following:
     * Class clazz1 = something;
     * Class class2 = somethingElse;
     * return class1.isAssignableFrom(class2);
     */
    public boolean isAssignableFrom(String type1, String type2) {
        if ("java/lang/Object".equals(type1))
            return true;
        if (type1.equals(type2))
            return true;

        getClassWrapper(type2);

        return buildAllSubClasses(type1).contains(type2);
    }

    private Set<String> buildAllSubClasses(String clazz) {

        ClassTree tree = getTree(clazz);
        if (tree == null)
            throw new RuntimeException(String.format(BUNDLE.getString("phantom-shield-x.instance.hierarchy-error"), clazz));

        if (tree.getAllSubClasses() == null) {
            Set<String> visited = new HashSet<>();
            for (String subClazz : tree.getSubClasses()) {
                if (visited.add(subClazz)) {
                    visited.addAll(buildAllSubClasses(subClazz));
                }
            }
            tree.setAllSubClasses(visited);
            return visited;
        } else {
            return tree.getAllSubClasses();
        }
    }

    /**
     * Finds {@link ClassWrapper} with given name.
     *
     * @return {@link ClassWrapper}.
     * @throws RuntimeException if not found.
     */
    public ClassWrapper getClassWrapper(String ref) {
        if (!classpath.containsKey(ref))
            throw new RuntimeException(String.format(BUNDLE.getString("phantom-shield-x.instance.hierarchy-error-2"), ref));

        return classpath.get(ref);
    }

    /**
     * Finds {@link ClassTree} with given name.
     *
     * @return {@link ClassTree}.
     * @throws RuntimeException if there are missing classes needed to build the inheritance tree.
     */
    public ClassTree getTree(String ref) {
        if (!hierarchy.containsKey(ref)) {
            ClassWrapper wrapper = getClassWrapper(ref);
            buildHierarchy(wrapper, null);
        }

        return hierarchy.get(ref);
    }

    public void buildHierarchy(ClassWrapper wrapper, ClassWrapper sub) {
        if (hierarchy.get(wrapper.getName()) == null) {
            ClassTree tree = new ClassTree(wrapper);

            if (wrapper.getSuperName() != null && !"java/lang/Object".equals(wrapper.getSuperName())) {
                tree.getParentClasses().add(wrapper.getSuperName());

                buildHierarchy(getClassWrapper(wrapper.getSuperName()), wrapper);
            }
            if (wrapper.getInterfaces() != null)
                wrapper.getInterfaces().forEach(s -> {
                    tree.getParentClasses().add(s);

                    buildHierarchy(getClassWrapper(s), wrapper);
                });

            hierarchy.put(wrapper.getName(), tree);
        }

        if (sub != null)
            hierarchy.get(wrapper.getName()).getSubClasses().add(sub.getName());
    }

    public void buildInheritance() {
        classes.values().forEach(classWrapper -> {
            try {
                buildHierarchy(classWrapper, null);
            } catch (Exception e) {
                ERROR(classWrapper.getName());
                throw e;
            }
        });
    }

    public Dictionary getDictionary() {
        return dictionary;
    }

    public TransformerRegister getRegister() {
        return register;
    }

    public long getSeed() {
        return seed;
    }

    public Map<String, ClassWrapper> getSoftExclusions() {
        return softExclusions;
    }

    public boolean isPrintClassesAsDirectory() {
        return printClassesAsDirectory;
    }

    public static void INFO(String message, Object... arguments) {
        LOGGER.info(message, arguments);
    }

    public static void INFO(String message, Object argument) {
        LOGGER.info(message, argument);
    }

    public static void INFO(String message) {
        LOGGER.info(message);
    }

    public static void WARN(String s) {
        LOGGER.warn(s);
    }

    public static void WARN(String s, Object o) {
        LOGGER.warn(s, o);
    }

    public static void WARN(String s, Object... objects) {
        LOGGER.warn(s, objects);
    }

    public static void WARN(String s, Throwable throwable) {
        LOGGER.warn(s, throwable);
    }

    public static void ERROR(String s) {
        LOGGER.error(s);
    }


    public static void ERROR(String s, Object o) {
        LOGGER.error(s, o);
    }

    public static void ERROR(String s, Object... objects) {
        LOGGER.error(s, objects);
    }

    public static void ERROR(String s, Throwable throwable) {
        LOGGER.error(s, throwable);
    }

    public static void ERROR(Throwable throwable) {
        LOGGER.error("", throwable);
    }

    public static String TRANSLATION(String key) {
        return BUNDLE.getString(key);
    }

}
