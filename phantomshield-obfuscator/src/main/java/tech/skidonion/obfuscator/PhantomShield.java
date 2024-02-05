package tech.skidonion.obfuscator;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.skidonion.obfuscator.asm.ClassTree;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.cpp.CompilerUpdater;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.transformer.TransformerRegister;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PhantomShield {
    public static final String VERSION = "0.0.1";
    public static final Logger LOGGER = LoggerFactory.getLogger(PhantomShield.class);
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public final Map<String, ClassWrapper> classes = new HashMap<>();
    public final Map<String, ClassWrapper> classpath = new HashMap<>();
    public final Map<String, byte[]> resources = new HashMap<>();
    private final Map<String, ClassTree> hierarchy = new HashMap<>();
    private final Config config;

    private CppCompiler compiler;

    public PhantomShield(File file) throws IOException {
        this(Config.readConfig(file));
    }

    public PhantomShield(Config config) {
        this.config = config;
    }


    public void process() {
        INFO("Java Home: {}", System.getProperty("java.home"));
        INFO("Phantom Shield X {}\n{}\n{}", VERSION, "Copyright 2019-2024 fl0wowp4rty", "All rights reserved");

        if (config.has("cpp_compiler")) {
            compiler = new CppCompiler(config.getAsJsonPrimitive("cpp_compiler").getAsString());
        } else {
            compiler = new CppCompiler(CompilerUpdater.DEFAULT_COMPILER);
        }
        compiler.init(this);
        if (config.has("cpp_compiler_arguments"))
            compiler.setExtraCommandLine(config.getAsJsonPrimitive("cpp_compiler_arguments").getAsString());
        if (config.has("cpp_compiler_output"))
            compiler.setDefaultOutput(config.getAsJsonPrimitive("cpp_compiler_output").getAsString());
        if (config.has("targets")) {
            JsonArray targets = config.getAsJsonArray("targets");
            targets.forEach(jsonElement -> compiler.addTarget(jsonElement.getAsString()));
        }

        loadClassPath();
        loadInput();

        TransformerRegister register = new TransformerRegister();
        register.parseConfig(config);
        register.process(this);

        writeOutput();
    }

    private void writeOutput() {
        File output = new File(config.getAsJsonPrimitive("output").getAsString());
        INFO(String.format("Writing output to \"%s\".", output.getAbsolutePath()));


        if (output.exists())
            INFO(String.format("Output file already exists, renamed to %s.", FileUtils.renameExistingFile(output)));

        try {

            DateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
            JsonPrimitive creationDate = config.getAsJsonPrimitive("creation_date");
            long timestamp = creationDate != null ? formatter.parse(creationDate.getAsString()).getTime() : -1;

            ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(output.toPath()));
            classes.values().forEach(classWrapper -> {
                try {
                    ZipEntry entry = new ZipEntry(classWrapper.getEntryName());
                    entry.setTime(timestamp);
                    zos.putNextEntry(entry);
                    zos.write(classWrapper.toByteArray(this));
                    zos.closeEntry();
                } catch (IOException ioe) {
                    LOGGER.error(String.format("Error writing class %s. Skipping.", classWrapper.getName() + ".class"));
                    ioe.printStackTrace();
                }
            });

            resources.forEach((name, bytes) -> {
                try {
                    ZipEntry entry = new ZipEntry(name);
                    zos.putNextEntry(entry);
                    zos.write(bytes);
                    zos.closeEntry();
                } catch (IOException ioe) {
                    LOGGER.error(String.format("Error writing resource %s. Skipping.", name));
                    ioe.printStackTrace();
                }
            });
            zos.setComment(String.format("Phantom Shield X %s\n%s", VERSION, "https://obfuscator.fl0wowp4rty.top/"));
            zos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException();
        } catch (ParseException pe) {
            pe.printStackTrace();
            throw new RuntimeException(pe);
        }
    }

    private void loadClassPath() {
        JsonArray libraries = config.getAsJsonArray("libraries");
        if (libraries != null) {
            for (JsonElement library : libraries) {
                if (library.isJsonPrimitive()) {
                    JsonPrimitive primitive = library.getAsJsonPrimitive();
                    if (primitive.isString()) {
                        File file = new File(primitive.getAsString());
                        if (file.exists()) {
                            INFO(String.format("Loading library \"%s\".", file.getAbsolutePath()));
                            try {
                                ZipFile zipFile = new ZipFile(file);
                                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                                while (entries.hasMoreElements()) {
                                    ZipEntry entry = entries.nextElement();

                                    if (!entry.isDirectory() && entry.getName().endsWith(".class"))
                                        try {
                                            ClassWrapper cw = new ClassWrapper(new ClassReader(zipFile.getInputStream(entry)), true);
                                            classpath.put(cw.getName(), cw);
                                        } catch (Throwable t) {
                                            LOGGER.error(String.format("Error while loading library class \"%s\".", entry.getName().replace(".class", "")));
                                            t.printStackTrace();
                                        }
                                }
                            } catch (ZipException e) {
                                LOGGER.error(String.format("Library \"%s\" could not be opened as a zip file.", file.getAbsolutePath()));
                                e.printStackTrace();
                            } catch (IOException e) {
                                LOGGER.error(String.format("IOException happened while trying to load classes from \"%s\".", file.getAbsolutePath()));
                                e.printStackTrace();
                            }
                        } else
                            LOGGER.error(String.format("Library \"%s\" could not be found and will be ignored.", file.getAbsolutePath()));

                    }
                }
            }
        }
    }

    private void loadInput() {
        File input = new File(config.getAsJsonPrimitive("input").getAsString());

        if (input.exists()) {
            INFO(String.format("Loading input \"%s\".", input.getAbsolutePath()));

            try {
                ZipFile zipFile = new ZipFile(input);
                Enumeration<? extends ZipEntry> entries = zipFile.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    InputStream in = zipFile.getInputStream(entry);

                    if (!entry.isDirectory())
                        if (entry.getName().endsWith(".class"))
                            try {
                                ClassWrapper cw = new ClassWrapper(new ClassReader(in), false);

                                if (cw.getVersion() <= Opcodes.V1_5)
                                    for (int i = 0; i < cw.getMethods().size(); i++) {
                                        MethodNode methodNode = cw.getMethods().get(i).getMethodNode();
                                        JSRInlinerAdapter adapter = new JSRInlinerAdapter(methodNode, methodNode.access, methodNode.name, methodNode.desc, methodNode.signature, methodNode.exceptions.toArray(new String[0]));
                                        methodNode.accept(adapter);
                                        cw.getMethods().get(i).setMethodNode(adapter);
                                    }

                                classpath.put(cw.getName(), cw);
                                classes.put(cw.getName(), cw);

                                String entryName = entry.getName();
                                String wrapperEntryName = cw.getEntryName();
                                if (entryName.endsWith(wrapperEntryName) && !entryName.equals(wrapperEntryName))
                                    cw.setEntryPrefix(entry.getName().substring(0, entryName.length() - wrapperEntryName.length()));
                            } catch (Throwable t) {
                                LOGGER.warn(String.format("Could not load %s as a class.", entry.getName()));
                                this.resources.put(entry.getName(), IOUtils.toByteArray(in));
                            }
                        else
                            this.resources.put(entry.getName(), IOUtils.toByteArray(in));
                }
            } catch (ZipException e) {
                LOGGER.error(String.format("Input file \"%s\" could not be opened as a zip file.", input.getAbsolutePath()));
                e.printStackTrace();
                throw new RuntimeException(e);
            } catch (IOException e) {
                LOGGER.error(String.format("IOException happened while trying to load classes from \"%s\".", input.getAbsolutePath()));
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            LOGGER.error(String.format("Unable to find \"%s\".", input.getAbsolutePath()));
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

        getClassWrapper(type1);
        getClassWrapper(type2);

        ClassTree firstTree = getTree(type1);
        if (firstTree == null)
            throw new RuntimeException("Could not find " + type1 + " in the built class hierarchy");

        Set<String> allChildren = new HashSet<>();
        Deque<String> toProcess = new ArrayDeque<>(firstTree.getSubClasses());
        while (!toProcess.isEmpty()) {
            String s = toProcess.poll();

            if (allChildren.add(s)) {
                getClassWrapper(s);
                ClassTree tempTree = getTree(s);
                toProcess.addAll(tempTree.getSubClasses());
            }
        }
        return allChildren.contains(type2);
    }

    /**
     * Finds {@link ClassWrapper} with given name.
     *
     * @return {@link ClassWrapper}.
     * @throws RuntimeException if not found.
     */
    public ClassWrapper getClassWrapper(String ref) {
        if (!classpath.containsKey(ref))
            throw new RuntimeException("Could not find " + ref);

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

    private void buildHierarchy(ClassWrapper wrapper, ClassWrapper sub) {
        if (hierarchy.get(wrapper.getName()) == null) {
            ClassTree tree = new ClassTree(wrapper);

            if (wrapper.getSuperName() != null) {
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
}
