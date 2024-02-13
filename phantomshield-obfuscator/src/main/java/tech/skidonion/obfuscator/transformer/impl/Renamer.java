package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.asm.ClassTree;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.remapper.ClassRemapper;
import tech.skidonion.obfuscator.asm.remapper.MemberRemapper;
import tech.skidonion.obfuscator.asm.remapper.Remapper;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ClassPackageValue;
import tech.skidonion.obfuscator.value.impls.StringArrayValue;
import tech.skidonion.obfuscator.value.impls.StringValue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class Renamer extends Transformer {
    private final BooleanValue importExistingMappings = new BooleanValue("import_existing_mappings", false);
    private final StringValue inputMappingsFiles = new StringValue("input_mappings_file", "mappings.txt");
    private final BooleanValue printMappings = new BooleanValue("print_mappings", false);
    private final StringValue printMappingsFile = new StringValue("print_mappings_file", "mappings.txt");
    private final BooleanValue repackage = new BooleanValue("repackage", false);
    private final ClassPackageValue repackageName = new ClassPackageValue("repackage_name", "skidonion/??????");
    private final StringArrayValue adaptResources = new StringArrayValue("adapt_resources");
    private final Map<String, String> methodMappings = new HashMap<>();
    private final Map<String, String> fieldMappings = new HashMap<>();
    private final Map<String, String> classMappings = new HashMap<>();
    private final Map<String, String> packageMappings = new HashMap<>();
    private final Map<String, String> dummy = new HashMap<>();

    public Renamer(String name) {
        super(name);
        addSettings(importExistingMappings, inputMappingsFiles, printMappings, printMappingsFile, repackage, repackageName, adaptResources);
    }

    private static boolean methodCanBeRenamed(MethodWrapper wrapper) {
        return !wrapper.getAccess().isNative() && !"main".equals(wrapper.getOriginalName())
                && !"premain".equals(wrapper.getOriginalName()) && !wrapper.getOriginalName().startsWith("<");
    }

    @Override
    public void transform() {
        obfuscator.buildInheritance();

        if (importExistingMappings.isEnable())
            readInputMappingsFile();

        Dictionary classDictionary = obfuscator.getDictionary().copy();
        Dictionary methodDictionary = obfuscator.getDictionary().copy();
        Dictionary fieldDictionary = obfuscator.getDictionary().copy();
        Dictionary packageDictionary = obfuscator.getDictionary().copy();

        INFO("Generating mappings.");
        long current = System.currentTimeMillis();

        getClassWrappers().forEach(classWrapper -> {
            classWrapper.getMethods().stream().filter(Renamer::methodCanBeRenamed).forEach(methodWrapper -> {
                HashSet<String> visited = new HashSet<>();

                if (!cannotRenameMethod(obfuscator.getTree(classWrapper.getOriginalName()), methodWrapper, visited))
                    genMethodMappings(methodWrapper, methodWrapper.getOwner().getOriginalName(), methodDictionary.nextUniqueString(), classWrapper.getAccess());
            });

            classWrapper.getFields().forEach(fieldWrapper -> {
                HashSet<String> visited = new HashSet<>();

                if (!cannotRenameField(obfuscator.getTree(classWrapper.getOriginalName()), fieldWrapper, visited))
                    genFieldMappings(fieldWrapper, fieldWrapper.getOwner().getOriginalName(), fieldDictionary.nextUniqueString());
            });

            if (match(classWrapper)) {
                String newName;

                if (repackage.isEnable()) {
                    newName = repackageName.getValue();
                } else {
                    String currentPackageName = classWrapper.getPackageName();
                    newName = packageMappings.get(currentPackageName);
                    if (newName == null) {
                        StringBuilder packageName = new StringBuilder(currentPackageName);
                        int index = 0;
                        StringBuilder lastPackageName = new StringBuilder();
                        while ((index = packageName.indexOf("/", index + 1)) != -1) {
                            String subpackage = packageName.substring(0, index + 1);
                            String mappedPackageName = packageMappings.get(subpackage);
                            if (mappedPackageName == null) {
                                lastPackageName.append(packageDictionary.nextUniqueString());
                                packageMappings.putIfAbsent(subpackage, lastPackageName.toString());
                                lastPackageName.append('/');
                            } else {
                                lastPackageName = new StringBuilder(mappedPackageName).append('/');
                            }
                        }
                        newName = lastPackageName.deleteCharAt(lastPackageName.length() - 1).toString();
                    }
                }
                if (newName.isEmpty()) {
                    newName = classDictionary.nextUniqueString();
                } else {
                    newName += '/' + classDictionary.nextUniqueString();
                }

                classMappings.put(classWrapper.getOriginalName(), newName);
            }
        });

        INFO("Finished generated mappings. [{}ms]", System.currentTimeMillis() - current);
        INFO("Applying mappings.");
        current = System.currentTimeMillis();

        dummy.putAll(classMappings);
        dummy.putAll(methodMappings);
        dummy.putAll(fieldMappings);
        dummy.putAll(packageMappings);

        // Apply mappings
        Remapper simpleRemapper = new MemberRemapper(dummy);
        new ArrayList<>(getClassWrappers()).forEach(classWrapper -> {
            ClassNode classNode = classWrapper.getClassNode();

            ClassNode copy = new ClassNode();
            classNode.accept(new ClassRemapper(copy, simpleRemapper));

            // In order to preserve the original names to prevent exclusions from breaking,
            // we update the MethodNode/FieldNode/ClassNode each wrapper wraps instead.
            IntStream.range(0, copy.methods.size())
                    .forEach(i -> classWrapper.getMethods().get(i).setMethodNode(copy.methods.get(i)));
            IntStream.range(0, copy.fields.size())
                    .forEach(i -> classWrapper.getFields().get(i).setFieldNode(copy.fields.get(i)));

            classWrapper.setClassNode(copy);

            getClasses().remove(classWrapper.getOriginalName());
            getClasses().put(classWrapper.getName(), classWrapper);
            getClassPath().put(classWrapper.getName(), classWrapper);
        });

        INFO("Mapped {} members. [{}ms]", dummy.size(), System.currentTimeMillis() - current);
        current = System.currentTimeMillis();

        // Now we gotta fix those resources because we probably screwed up random files.
        INFO("Attempting to map class names in resources");
        AtomicInteger fixed = new AtomicInteger();
        getResources().forEach((name, byteArray) -> adaptResources.getValue().forEach(s -> {
            Pattern pattern = Pattern.compile(s);

            if (pattern.matcher(name).matches()) {
                String stringVer = new String(byteArray, StandardCharsets.UTF_8);

                for (String mapping : classMappings.keySet()) {
                    String original = mapping.replace("/", ".");
                    if (stringVer.contains(original)) {
                        // Regex that ensures that class names that match words in the manifest don't break the
                        // manifest.
                        // Example: name == Main
                        if ("META-INF/MANIFEST.MF".equals(name) // Manifest
                                || "plugin.yml".equals(name) // Spigot plugin
                                || "bungee.yml".equals(name)) // Bungeecord plugin
                            stringVer = stringVer.replaceAll("(?<=[: ])" + original,
                                    classMappings.get(mapping).replace("/", "."));
                        else
                            stringVer = stringVer.replace(original, classMappings.get(mapping).replace("/", "."));
                    }
                }

                getResources().put(name, stringVer.getBytes(StandardCharsets.UTF_8));
                fixed.incrementAndGet();
            }
        }));

        INFO("Mapped {} names in resources. [{}ms]", fixed.get(), System.currentTimeMillis() - current);

        if (printMappings.isEnable())
            printMappings();
    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return Type.getDescriptor(tech.skidonion.obfuscator.annotations.Renamer.class);
    }

    private void readInputMappingsFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(inputMappingsFiles.getValue()))) {
            for (String line; (line = reader.readLine()) != null; ) {
                String[] split = line.split(" -> ");
                if (split.length != 2) {
                    throw new IndexOutOfBoundsException();
                }
                if (split[0].contains("(")) {
                    methodMappings.put(split[0], split[1]);
                } else if (split[0].contains(".")) {
                    fieldMappings.put(split[0], split[1]);
                } else if (split[0].endsWith("/")) {
                    classMappings.put(split[0], split[1]);
                } else {
                    packageMappings.put(split[0], split[1]);
                }
            }
        } catch (FileNotFoundException e) {
            ERROR("Mappings file not found. Skipping import.");
        } catch (IOException e) {
            ERROR("Ran into an error trying to read the mappings file.", e);
        } catch (IndexOutOfBoundsException e) {
            ERROR("Invalid mappings file format. Skipping import.");
        }
    }

    private void genMethodMappings(MethodWrapper methodWrapper, String owner, String newName, Access access) {
        String key = owner + '.' + methodWrapper.getOriginalName() + methodWrapper.getOriginalDescription();

        // This (supposedly) will always stop the recursion because the tree was already renamed
        if (methodMappings.containsKey(key))
            return;

        ClassTree tree = obfuscator.getTree(owner);

        methodMappings.put(key, newName);
        if (access.isAnnotation()) {
            methodMappings.put(StringUtils.toDescriptor(owner) + '.' + methodWrapper.getOriginalName(), newName);
        }

        if (!methodWrapper.getAccess().isStatic()) { // Static methods can't be overridden
            tree.getParentClasses().forEach(parentClass -> genMethodMappings(methodWrapper, parentClass, newName, access));
            tree.getSubClasses().forEach(subClass -> genMethodMappings(methodWrapper, subClass, newName, access));
        }
    }

    private boolean cannotRenameMethod(ClassTree tree, MethodWrapper wrapper, Set<String> visited) {
        String check = tree.getClassWrapper().getOriginalName() + '.' + wrapper.getOriginalName() + wrapper.getOriginalDescription();

        // Don't check these
        if (visited.contains(check))
            return false;

        visited.add(check);

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (!match(wrapper) || methodMappings.containsKey(check))
            return true;

        // Methods which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode()
                    && tree.getClassWrapper().getMethods().stream().anyMatch(mw -> mw.getOriginalName().equals(wrapper.getOriginalName())
                    && mw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameMethod(obfuscator.getTree(parent), wrapper, visited))
                    || (tree.getSubClasses().stream().anyMatch(sub -> cannotRenameMethod(obfuscator.getTree(sub), wrapper, visited)));
        } else {
            return tree.getClassWrapper().getAccess().isEnum()
                    && ("valueOf".equals(wrapper.getOriginalName()) || "values".equals(wrapper.getOriginalName()));
        }
    }

    private void genFieldMappings(FieldWrapper fieldWrapper, String owner, String newName) {
        // This (supposedly) will always stop the recursion because the tree was already renamed
        if (fieldMappings.containsKey(owner + '.' + fieldWrapper.getOriginalName() + '.' + fieldWrapper.getOriginalDescription()))
            return;

        ClassTree tree = obfuscator.getTree(owner);

        fieldMappings.put(owner + '.' + fieldWrapper.getOriginalName() + '.' + fieldWrapper.getOriginalDescription(), newName);

        if (!fieldWrapper.getAccess().isStatic()) { // Static fields can't be inherited
            tree.getParentClasses().forEach(parentClass -> genFieldMappings(fieldWrapper, parentClass, newName));
            tree.getSubClasses().forEach(subClass -> genFieldMappings(fieldWrapper, subClass, newName));
        }
    }

    private boolean cannotRenameField(ClassTree tree, FieldWrapper wrapper, Set<String> visited) {
        String check = tree.getClassWrapper().getOriginalName() + '.' + wrapper.getOriginalName() + '.' + wrapper.getOriginalDescription();

        // Don't check these
        if (visited.contains(check))
            return false;

        visited.add(check);

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (!match(wrapper) || fieldMappings.containsKey(check))
            return true;

        // Fields which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode()
                    && tree.getClassWrapper().getFields().stream().anyMatch(fw -> fw.getOriginalName().equals(wrapper.getOriginalName())
                    && fw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameField(obfuscator.getTree(parent), wrapper, visited))
                    || (tree.getSubClasses().stream().anyMatch(sub -> cannotRenameField(obfuscator.getTree(sub), wrapper, visited)));
        }

        return false;
    }

    private void printMappings() {
        long current = System.currentTimeMillis();
        INFO("Printing mappings.");
        File file = new File(printMappingsFile.getValue());
        if (file.exists())
            FileUtils.renameExistingFile(file);

        try {
            file.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            dummy.forEach((oldName, newName) -> {
                try {
                    bw.append(oldName).append(" -> ").append(newName).append('\n');
                } catch (IOException ioe) {
                    ERROR("Ran into an error trying to append \"{} -> {}\"", oldName, newName);
                    ioe.printStackTrace();
                }
            });

            bw.close();
            INFO("Finished dumping mappings at {}. [{}ms]", file.getAbsolutePath(),
                    System.currentTimeMillis() - current);
        } catch (Throwable t) {
            ERROR("Ran into an error trying to create the mappings file.");
            t.printStackTrace();
        }
    }

}
