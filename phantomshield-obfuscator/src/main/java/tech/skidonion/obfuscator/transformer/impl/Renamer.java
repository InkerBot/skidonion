package tech.skidonion.obfuscator.transformer.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.asm.ClassTree;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.asm.remapper.ClassRemapper;
import tech.skidonion.obfuscator.asm.remapper.MemberRemapper;
import tech.skidonion.obfuscator.asm.remapper.Remapper;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.renamer.RenamerResult;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ClassPackageValue;
import tech.skidonion.obfuscator.value.impls.StringArrayValue;
import tech.skidonion.obfuscator.value.impls.StringValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class Renamer extends Transformer {
    private final BooleanValue printMappings = new BooleanValue("print_mappings", false);
    private final StringValue printMappingsFile = new StringValue("print_mappings_file", "mappings.txt");
    private final BooleanValue repackage = new BooleanValue("repackage", false);
    private final ClassPackageValue repackageName = new ClassPackageValue("repackage_name", "skidonion/??????");
    private final StringArrayValue adaptResources = new StringArrayValue("adapt_resources");
    private final Map<String, String> methodMappings = new HashMap<>();
    final Set<String> methodsObfuscated = new HashSet<>();
    private final Map<String, String> fieldMappings = new HashMap<>();
    final Set<String> fieldsObfuscated = new HashSet<>();
    private final Map<String, String> classMappings = new HashMap<>();
    private final Map<String, String> packageMappings = new HashMap<>();
    private final Map<String, String> annotationMappings = new HashMap<>();
    private final Map<String, String> dummy = new HashMap<>();
    private final Map<String, String> mappings = new HashMap<>();

    public Renamer(String name) {
        super(name);
        addSettings(printMappings, printMappingsFile, repackage, repackageName, adaptResources);
    }

    private static boolean methodCanBeRenamed(MethodWrapper wrapper) {
        return !wrapper.getAccess().isNative() && !"main".equals(wrapper.getOriginalName())
                && !"premain".equals(wrapper.getOriginalName()) && !wrapper.getOriginalName().startsWith("<");
    }

    @Override
    public void transform() {
        INFO("Generating mappings.");
        long current = System.currentTimeMillis();

        getClassWrappers().forEach(classWrapper -> {
            final Set<String> generated = new HashSet<>();
            classWrapper.getMethods().stream().filter(Renamer::methodCanBeRenamed).forEach(methodWrapper -> {
                removeAnnotation(methodWrapper);
                Set<String> visited = new HashSet<>();

                if (!cannotRenameMethod(obfuscator.getTree(classWrapper.getOriginalName()), methodWrapper, visited)) {
                    processRenamerResult(genMethodMappings(methodWrapper, methodWrapper.getOwner().getOriginalName(), new RenamerResult(classWrapper.getMethodDictionary().nextUniqueString(), 0), generated));
                }
            });

            classWrapper.getFields().forEach(fieldWrapper -> {
                removeAnnotation(fieldWrapper);
                Set<String> visited = new HashSet<>();

                if (!cannotRenameField(obfuscator.getTree(classWrapper.getOriginalName()), fieldWrapper, visited)) {
                    processRenamerResult(genFieldMappings(fieldWrapper, fieldWrapper.getOwner().getOriginalName(), new RenamerResult(classWrapper.getFieldDictionary().nextUniqueString(), 0), generated));
                }
            });

            if (match(classWrapper)) {
                removeAnnotation(classWrapper);

                String currentPackageName = classWrapper.getPackageName();
                Dictionary classDictionary = obfuscator.classesDictionaries.computeIfAbsent(currentPackageName, packageName -> obfuscator.getDictionary().copy());

                String newName;
                if (repackage.isEnable()) {
                    newName = repackageName.getValue();
                } else {
                    newName = packageMappings.computeIfAbsent(currentPackageName, package_name -> {
                        StringBuilder packageName = new StringBuilder(package_name);
                        int index = 0;
                        StringBuilder lastPackageName = new StringBuilder();
                        while ((index = packageName.indexOf("/", index + 1)) != -1) {
                            String subpackage = packageName.substring(0, index + 1); // give subpackage mapping
                            // while mapping subpackage,
                            // it must use the sub subpackage's dictionary
                            String dictionaryPackage = packageName.substring(0, index);
                            Dictionary packageDictionary = obfuscator.packageDictionaries.computeIfAbsent(dictionaryPackage.substring(0, dictionaryPackage.lastIndexOf("/") + 1), subpackage_name -> obfuscator.getDictionary().copy());
                            String mappedPackageName = packageMappings.get(subpackage);
                            if (mappedPackageName == null) {
                                lastPackageName.append(packageDictionary.nextUniqueString()).append("/");
                                packageMappings.putIfAbsent(subpackage, lastPackageName.toString());
                            } else {
                                lastPackageName = new StringBuilder(mappedPackageName);
                            }
                        }
                        return lastPackageName.toString();
                    });
                }
                newName += classDictionary.nextUniqueString();
                classMappings.put(classWrapper.getOriginalName(), newName);
            }
        });

        INFO("Finished generated mappings. [{}ms]", System.currentTimeMillis() - current);
        INFO("Applying mappings.");
        current = System.currentTimeMillis();

        mappings.putAll(classMappings);
        mappings.putAll(methodMappings);
        mappings.putAll(fieldMappings);
        mappings.putAll(packageMappings);
        mappings.putAll(annotationMappings);
        mappings.putAll(dummy);

        // Apply mappings
        Remapper simpleRemapper = new MemberRemapper(mappings);
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
            classWrapper.updateMemberNames();

            getClasses().remove(classWrapper.getOriginalName());
            getClasses().put(classWrapper.getName(), classWrapper);
            getClassPath().put(classWrapper.getName(), classWrapper);
        });

        INFO("Mapped {} members. [{}ms]", mappings.size(), System.currentTimeMillis() - current);
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

    private void processRenamerResult(RenamerResult result) {
        String obfuscatedName = result.getObfuscatedName();
        for (Map.Entry<String, RenamerResult.RenamerType> entry : result.getInfluences().entrySet()) {
            switch (entry.getValue()) {
                case FIELD:
                    fieldMappings.put(entry.getKey(), obfuscatedName);
                    break;
                case METHOD:
                    methodMappings.put(entry.getKey(), obfuscatedName);
                    break;
                case OBFUSCATED_FIELD:
                    fieldsObfuscated.add(String.format(entry.getKey(), obfuscatedName));
                    break;
                case OBFUSCATED_METHOD:
                    methodsObfuscated.add(String.format(entry.getKey(), obfuscatedName));
                    break;
                case ANNOTATION:
                    annotationMappings.put(entry.getKey(), obfuscatedName);
                    break;
                case DUMMY:
                    dummy.put(entry.getKey(), obfuscatedName);
                    break;
                default:
                    throw new RuntimeException("impossible renamer type");
            }
        }
    }

    private RenamerResult genMethodMappings(MethodWrapper methodWrapper, String owner, RenamerResult result, Set<String> visited) {
        String uniqueMethodName = methodWrapper.getOriginalName() + methodWrapper.getOriginalDescription();
        String key = owner + '.' + uniqueMethodName;
        // ignore generated
        if (!visited.add(key)) return result;

        ClassTree tree = obfuscator.getTree(owner);
        ClassWrapper cw = tree.getClassWrapper();
        Dictionary dictionary = cw.getMethodDictionary();
        String obfuscatedKey = owner + ".%s" + methodWrapper.getOriginalDescription();
        if (methodsObfuscated.contains(String.format(obfuscatedKey, result.getObfuscatedName()))) {
            int index = Math.max(dictionary.getUniqueIndex(), result.getMaximumIndex());
            dictionary.setUniqueIndex(index);
            result.setObfuscatedName(dictionary.nextUniqueString());
            result.setMaximumIndex(dictionary.getUniqueIndex());
        }

        if (cw.getMethodDescriptors().contains(uniqueMethodName)) {
            result.add(key, RenamerResult.RenamerType.METHOD);
            result.add(obfuscatedKey, RenamerResult.RenamerType.OBFUSCATED_METHOD);
            if (cw.getAccess().isAnnotation()) {
                result.add(StringUtils.toDescriptor(owner) + '.' + methodWrapper.getOriginalName(), RenamerResult.RenamerType.ANNOTATION);
            }
        } else {
            result.add(key, RenamerResult.RenamerType.DUMMY);
        }


        if (!methodWrapper.getAccess().isStatic()) { // Static methods can't be overridden
            tree.getParentClasses().forEach(parentClass -> genMethodMappings(methodWrapper, parentClass, result, visited));
            tree.getSubClasses().forEach(subClass -> genMethodMappings(methodWrapper, subClass, result, visited));
        }
        return result;
    }

    private RenamerResult genFieldMappings(FieldWrapper fieldWrapper, String owner, RenamerResult result, Set<String> visited) {
        String uniqueFieldName = fieldWrapper.getOriginalName() + '.' + fieldWrapper.getOriginalDescription();
        String key = owner + '.' + uniqueFieldName;
        if (!visited.add(key)) return result;

        ClassTree tree = obfuscator.getTree(owner);
        ClassWrapper cw = tree.getClassWrapper();
        Dictionary dictionary = cw.getFieldDictionary();
        String obfuscatedKey = owner + ".%s." + fieldWrapper.getOriginalDescription();
        if (fieldsObfuscated.contains(String.format(obfuscatedKey, result.getObfuscatedName()))) {
            int index = Math.max(dictionary.getUniqueIndex(), result.getMaximumIndex());
            dictionary.setUniqueIndex(index);
            result.setObfuscatedName(dictionary.nextUniqueString());
            result.setMaximumIndex(dictionary.getUniqueIndex());
        }

        if (cw.getFieldDescriptors().contains(uniqueFieldName)) {
            result.add(key, RenamerResult.RenamerType.FIELD);
            result.add(obfuscatedKey, RenamerResult.RenamerType.OBFUSCATED_FIELD);
        } else {
            result.add(key, RenamerResult.RenamerType.DUMMY);
        }

        if (!fieldWrapper.getAccess().isStatic()) { // Static fields can't be inherited
            tree.getParentClasses().forEach(parentClass -> genFieldMappings(fieldWrapper, parentClass, result, visited));
            tree.getSubClasses().forEach(subClass -> genFieldMappings(fieldWrapper, subClass, result, visited));
        }
        return result;
    }

    private boolean cannotRenameMethod(ClassTree tree, MethodWrapper wrapper, Set<String> visited) {
        String check = tree.getClassWrapper().getOriginalName() + '.' + wrapper.getOriginalName() + wrapper.getOriginalDescription();

        // Don't check these
        if (visited.contains(check))
            return false;

        visited.add(check);

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (methodMappings.containsKey(check) || !match(wrapper))
            return true;

        // Methods which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode()
                    && tree.getClassWrapper().getMethods().stream().anyMatch(mw -> mw.getOriginalName().equals(wrapper.getOriginalName())
                    && mw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameMethod(obfuscator.getTree(parent), wrapper, visited))
                    || tree.getSubClasses().stream().anyMatch(sub -> cannotRenameMethod(obfuscator.getTree(sub), wrapper, visited));
        } else {
            return tree.getClassWrapper().getAccess().isEnum()
                    && ("valueOf".equals(wrapper.getOriginalName()) || "values".equals(wrapper.getOriginalName()));
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
        if (fieldMappings.containsKey(check) || !match(wrapper))
            return true;

        // Fields which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode()
                    && tree.getClassWrapper().getFields().stream().anyMatch(fw -> fw.getOriginalName().equals(wrapper.getOriginalName())
                    && fw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameField(obfuscator.getTree(parent), wrapper, visited))
                    || tree.getSubClasses().stream().anyMatch(sub -> cannotRenameField(obfuscator.getTree(sub), wrapper, visited));
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
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final JsonObject mappings = new JsonObject();
            mappings.addProperty("version", "phantom-shield-x,1");

            final JsonObject packages = new JsonObject();
            final JsonObject rootPackage = new JsonObject();
            rootPackage.addProperty("unique_index", obfuscator.packageDictionaries.computeIfAbsent("", k -> obfuscator.getDictionary().copy()).getUniqueIndex());
            rootPackage.addProperty("class_unique_index", obfuscator.classesDictionaries.computeIfAbsent("", k -> obfuscator.getDictionary().copy()).getUniqueIndex());
            packages.add("", rootPackage);
            packageMappings.forEach((origin, obfuscated) -> {
                final JsonObject packageMapping = new JsonObject();
                packageMapping.addProperty("obfuscated", obfuscated);
                packageMapping.addProperty("unique_index", obfuscator.packageDictionaries.computeIfAbsent(origin, k -> obfuscator.getDictionary().copy()).getUniqueIndex());
                packageMapping.addProperty("class_unique_index", obfuscator.classesDictionaries.computeIfAbsent(origin, k -> obfuscator.getDictionary().copy()).getUniqueIndex());
                packages.add(origin, packageMapping);
            });
            mappings.add("packages", packages);

            Map<String, JsonObject> classesMethodMap = new HashMap<>();
            Map<String, JsonObject> classesFieldMap = new HashMap<>();
            final JsonObject classes = new JsonObject();
            classMappings.forEach((origin, obfuscated) -> {
                final JsonObject classMapping = new JsonObject();
                final JsonObject methods = new JsonObject();
                final JsonObject fields = new JsonObject();
                classMapping.add("methods", methods);
                classMapping.add("fields", fields);
                classesMethodMap.put(origin, methods);
                classesFieldMap.put(origin, fields);
                classMapping.addProperty("method_unique_index", obfuscator.getClassWrapper(origin).getMethodDictionary().getUniqueIndex());
                classMapping.addProperty("field_unique_index", obfuscator.getClassWrapper(origin).getFieldDictionary().getUniqueIndex());
                classMapping.addProperty("obfuscated", obfuscated);
                classes.add(origin, classMapping);
            });
            mappings.add("classes", classes);

            Map<String, JsonObject> annotationsMap = new HashMap<>();
            final JsonObject annotations = new JsonObject();
            annotationMappings.forEach((origin, obfuscated) -> {
                String[] parts = origin.split("\\.");
                if (parts.length != 2)
                    throw new RuntimeException("impossible annotation mapping: " + origin);
                final JsonObject annotationMapping = new JsonObject();
                final JsonObject values = annotationsMap.computeIfAbsent(parts[0], annotationName -> {
                    final JsonObject value = new JsonObject();
                    annotationMapping.add("values", value);
                    annotations.add(annotationName, annotationMapping);
                    return value;
                });
                values.addProperty(parts[1], obfuscated);
            });
            mappings.add("annotations", annotations);

            methodMappings.forEach((origin, obfuscated) -> {
                String[] parts = origin.split("\\.");
                if (parts.length != 2)
                    throw new RuntimeException("impossible method mapping: " + origin);
                final JsonObject methods = classesMethodMap.computeIfAbsent(parts[0], className -> {
                    final JsonObject classMapping = new JsonObject();
                    final JsonObject methodsMapping = new JsonObject();
                    final JsonObject fieldsMapping = new JsonObject();
                    classMapping.addProperty("method_unique_index", obfuscator.getClassWrapper(className).getMethodDictionary().getUniqueIndex());
                    classMapping.addProperty("field_unique_index", obfuscator.getClassWrapper(className).getFieldDictionary().getUniqueIndex());
                    classMapping.add("methods", methodsMapping);
                    classMapping.add("fields", fieldsMapping);
                    classesFieldMap.put(className, fieldsMapping);
                    classes.add(className, classMapping);
                    return methodsMapping;
                });
                methods.addProperty(parts[1], obfuscated);
            });

            fieldMappings.forEach((origin, obfuscated) -> {
                String[] parts = origin.split("\\.");
                if (parts.length != 3)
                    throw new RuntimeException("impossible method mapping: " + origin);
                final JsonObject fields = classesFieldMap.computeIfAbsent(parts[0], className -> {
                    final JsonObject classMapping = new JsonObject();
                    final JsonObject methodsMapping = new JsonObject();
                    final JsonObject fieldsMapping = new JsonObject();
                    classMapping.addProperty("method_unique_index", obfuscator.getClassWrapper(className).getMethodDictionary().getUniqueIndex());
                    classMapping.addProperty("field_unique_index", obfuscator.getClassWrapper(className).getFieldDictionary().getUniqueIndex());
                    classMapping.add("methods", methodsMapping);
                    classMapping.add("fields", fieldsMapping);
                    classesMethodMap.put(className, methodsMapping);
                    classes.add(className, classMapping);
                    return fieldsMapping;
                });
                fields.addProperty(parts[1] + "." + parts[2], obfuscated);
            });

            // TODO dummies should be computed automatically

//            final JsonObject dummies = new JsonObject();
//            Map<String, JsonObject> dummiesMethodMap = new HashMap<>();
//            Map<String, JsonObject> dummiesFieldMap = new HashMap<>();
//            dummy.forEach((origin, obfuscated) -> {
//                boolean field;
//                String[] parts = origin.split("\\.");
//                if (parts.length == 2) {
//                    field = false;
//                } else if (parts.length == 3) {
//                    field = true;
//                } else {
//                    throw new RuntimeException("impossible method mapping: " + origin);
//                }
//                if (field) {
//                    final JsonObject fields = dummiesFieldMap.computeIfAbsent(parts[0], className -> {
//                        final JsonObject classMapping = new JsonObject();
//                        final JsonObject methodsMapping = new JsonObject();
//                        final JsonObject fieldsMapping = new JsonObject();
//                        classMapping.add("methods", methodsMapping);
//                        classMapping.add("fields", fieldsMapping);
//                        dummiesMethodMap.put(className, methodsMapping);
//                        dummies.add(className, classMapping);
//                        return fieldsMapping;
//                    });
//                    fields.addProperty(parts[1] + "." + parts[2], obfuscated);
//                } else {
//                    final JsonObject methods = dummiesMethodMap.computeIfAbsent(parts[0], className -> {
//                        final JsonObject classMapping = new JsonObject();
//                        final JsonObject methodsMapping = new JsonObject();
//                        final JsonObject fieldsMapping = new JsonObject();
//                        classMapping.add("methods", methodsMapping);
//                        classMapping.add("fields", fieldsMapping);
//                        dummiesFieldMap.put(className, fieldsMapping);
//                        dummies.add(className, classMapping);
//                        return methodsMapping;
//                    });
//                    methods.addProperty(parts[1], obfuscated);
//                }
//            });
//            mappings.add("dummies", dummies);

            gson.toJson(mappings, bw);
            bw.close();
            INFO("Finished printing mappings at {}. [{}ms]", file.getAbsolutePath(),
                    System.currentTimeMillis() - current);
        } catch (Throwable t) {
            ERROR("Ran into an error trying to create the mappings file.", t);
        }
    }

}
