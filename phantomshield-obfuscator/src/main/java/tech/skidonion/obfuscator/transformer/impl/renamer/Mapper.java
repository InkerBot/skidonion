package tech.skidonion.obfuscator.transformer.impl.renamer;

import com.google.gson.*;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassTree;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.asm.remapper.ClassRemapper;
import tech.skidonion.obfuscator.asm.remapper.MemberRemapper;
import tech.skidonion.obfuscator.asm.remapper.Remapper;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.transformer.impl.Renamer;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.utils.StringUtils;

import java.io.*;
import java.util.*;
import java.util.stream.IntStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class Mapper {
    public static final String MAPPING_VERSION = "phantom-shield-x,1";

    private final PhantomShield obfuscator;
    private final Map<String, String> methodMappings = new HashMap<>();
    private final Map<String, String> fieldMappings = new HashMap<>();
    private final Map<String, String> classMappings = new HashMap<>();
    private final Map<String, String> packageMappings = new HashMap<>();
    private final Map<String, String> annotationMappings = new HashMap<>();
    private final Map<String, String> dummy = new HashMap<>();
    private final Map<String, String> mappings = new HashMap<>();
    private final Collection<ClassWrapper> classes;
    private final Renamer renamer;

    private String prefix_name = "";
    private boolean repackage = false;
    private String repakage_name = "";
    private boolean random_package = false;

    public Mapper(PhantomShield obfuscator, Collection<ClassWrapper> classes) {
        this(obfuscator, classes, null);
    }

    public Mapper(PhantomShield obfuscator, Collection<ClassWrapper> classes, Renamer renamer) {
        this.obfuscator = obfuscator;
        this.classes = classes;
        this.renamer = renamer;
    }

    public void generateMappings() {
        classes.forEach(classWrapper -> {
            final Set<String> generated = new HashSet<>();
            classWrapper.getMethods().stream().filter(Mapper::methodCanBeRenamed).forEach(methodWrapper -> {
                if (renamer != null) renamer.removeAnnotation(methodWrapper);

                Set<String> visited = new HashSet<>();

                if (!cannotRenameMethod(obfuscator.getTree(classWrapper.getOriginalName()), methodWrapper, visited)) {
                    RenamerResult result = genMethodMappings(methodWrapper, methodWrapper.getOwner().getOriginalName(), new RenamerResult(), generated);
                    classWrapper.getMethodDictionary().setUniqueIndex(result.getMaximumIndex());
                    result.setObfuscatedName(prefix_name + classWrapper.getMethodDictionary().nextUniqueString());
                    processRenamerResult(result);
                }
            });

            classWrapper.getFields().forEach(fieldWrapper -> {
                if (renamer != null) renamer.removeAnnotation(fieldWrapper);
                Set<String> visited = new HashSet<>();

                if (!cannotRenameField(obfuscator.getTree(classWrapper.getOriginalName()), fieldWrapper, visited)) {
                    RenamerResult result = genFieldMappings(fieldWrapper, fieldWrapper.getOwner().getOriginalName(), new RenamerResult(), generated);
                    classWrapper.getFieldDictionary().setUniqueIndex(result.getMaximumIndex());
                    result.setObfuscatedName(prefix_name + classWrapper.getFieldDictionary().nextUniqueString());
                    processRenamerResult(result);
                }
            });

            if (renamer == null || renamer.match(classWrapper)) {
                if (renamer != null) renamer.removeAnnotation(classWrapper);

                String currentPackageName = classWrapper.getPackageName();
                Dictionary classDictionary;

                String newName;
                if (random_package) {
                    List<ClassWrapper> wrappers = new ArrayList<>(obfuscator.classes.values());
                    String randomPackage = wrappers.get(RandomUtils.getRandomInt(wrappers.size())).getPackageName();
                    classDictionary = obfuscator.classesDictionaries.computeIfAbsent(randomPackage, packageName -> obfuscator.getDictionary().copy());
                    newName = randomPackage;
                } else if (repackage) {
                    classDictionary = obfuscator.classesDictionaries.computeIfAbsent("", packageName -> obfuscator.getDictionary().copy());
                    newName = repakage_name;
                } else {
                    classDictionary = obfuscator.classesDictionaries.computeIfAbsent(currentPackageName, packageName -> obfuscator.getDictionary().copy());
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
                                lastPackageName.append(prefix_name).append(packageDictionary.nextUniqueString()).append("/");
                                packageMappings.putIfAbsent(subpackage, lastPackageName.toString());
                            } else {
                                lastPackageName = new StringBuilder(mappedPackageName);
                            }
                        }
                        return lastPackageName.toString();
                    });
                }
                newName += prefix_name;
                newName += classDictionary.nextUniqueString();
                classMappings.putIfAbsent(classWrapper.getOriginalName(), newName);
            }
        });
    }

    public void apply() {
        mappings.putAll(classMappings);
        mappings.putAll(methodMappings);
        mappings.putAll(fieldMappings);
        mappings.putAll(packageMappings);
        mappings.putAll(annotationMappings);
        mappings.putAll(dummy);

        // Apply mappings
        Remapper simpleRemapper = new MemberRemapper(mappings);
        new ArrayList<>(obfuscator.classes.values()).forEach(classWrapper -> {
            ClassNode classNode = classWrapper.getClassNode();

            ClassNode copy = new ClassNode();
            classNode.accept(new ClassRemapper(copy, simpleRemapper));

            // In order to preserve the original names to prevent exclusions from breaking,
            // we update the MethodNode/FieldNode/ClassNode each wrapper wraps instead.
            IntStream.range(0, copy.methods.size()).forEach(i -> classWrapper.getMethods().get(i).setMethodNode(copy.methods.get(i)));
            IntStream.range(0, copy.fields.size()).forEach(i -> classWrapper.getFields().get(i).setFieldNode(copy.fields.get(i)));
            classWrapper.setClassNode(copy);
            classWrapper.updateMemberNames();

            obfuscator.classes.remove(classWrapper.getOriginalName());
            obfuscator.classes.put(classWrapper.getName(), classWrapper);
            obfuscator.classpath.put(classWrapper.getName(), classWrapper);
        });
    }


    private void processRenamerResult(RenamerResult result) {
        String obfuscatedName = result.getObfuscatedName();
        for (Map.Entry<String, RenamerResult.RenamerType> entry : result.getInfluences().entrySet()) {
            switch (entry.getValue()) {
                case FIELD:
                    fieldMappings.putIfAbsent(entry.getKey(), obfuscatedName);
                    break;
                case METHOD:
                    methodMappings.putIfAbsent(entry.getKey(), obfuscatedName);
                    break;
                case ANNOTATION:
                    annotationMappings.putIfAbsent(entry.getKey(), obfuscatedName);
                    break;
                case DUMMY:
                    dummy.putIfAbsent(entry.getKey(), obfuscatedName);
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
        if (!visited.add(key) || methodMappings.containsKey(key)) return result;

        ClassTree tree = obfuscator.getTree(owner);
        ClassWrapper cw = tree.getClassWrapper();
        Dictionary dictionary = cw.getMethodDictionary();
        result.setMaximumIndex(Math.max(dictionary.getUniqueIndex(), result.getMaximumIndex()));

        if (cw.getMethodDescriptors().contains(uniqueMethodName)) {

            result.add(key, RenamerResult.RenamerType.METHOD);
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
        if (!visited.add(key) || fieldMappings.containsKey(key)) return result;

        ClassTree tree = obfuscator.getTree(owner);
        ClassWrapper cw = tree.getClassWrapper();
        Dictionary dictionary = cw.getFieldDictionary();
        result.setMaximumIndex(Math.max(dictionary.getUniqueIndex(), result.getMaximumIndex()));

        if (cw.getFieldDescriptors().contains(uniqueFieldName)) {
            result.add(key, RenamerResult.RenamerType.FIELD);
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
        if (visited.contains(check)) return false;

        visited.add(check);

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (methodMappings.containsKey(check) || (renamer != null && !renamer.match(wrapper))) return true;

        // Methods which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode() && tree.getClassWrapper().getMethods().stream().anyMatch(mw -> mw.getOriginalName().equals(wrapper.getOriginalName()) && mw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameMethod(obfuscator.getTree(parent), wrapper, visited)) || tree.getSubClasses().stream().anyMatch(sub -> cannotRenameMethod(obfuscator.getTree(sub), wrapper, visited));
        } else {
            return tree.getClassWrapper().getAccess().isEnum() && ("valueOf".equals(wrapper.getOriginalName()) || "values".equals(wrapper.getOriginalName()));
        }
    }

    private boolean cannotRenameField(ClassTree tree, FieldWrapper wrapper, Set<String> visited) {
        String check = tree.getClassWrapper().getOriginalName() + '.' + wrapper.getOriginalName() + '.' + wrapper.getOriginalDescription();

        // Don't check these
        if (visited.contains(check)) return false;

        visited.add(check);

        // If excluded, we don't want to rename.
        // If we already mapped the tree, we don't want to waste time doing it again.
        if (fieldMappings.containsKey(check) || (renamer != null && !renamer.match(wrapper))) return true;

        // Fields which are static don't need to be checked for inheritance
        if (!wrapper.getAccess().isStatic()) {
            // We can't rename members which inherit methods from external libraries
            if (tree.getClassWrapper() != wrapper.getOwner() && tree.getClassWrapper().isLibraryNode() && tree.getClassWrapper().getFields().stream().anyMatch(fw -> fw.getOriginalName().equals(wrapper.getOriginalName()) && fw.getOriginalDescription().equals(wrapper.getOriginalDescription())))
                return true;

            return tree.getParentClasses().stream().anyMatch(parent -> cannotRenameField(obfuscator.getTree(parent), wrapper, visited)) || tree.getSubClasses().stream().anyMatch(sub -> cannotRenameField(obfuscator.getTree(sub), wrapper, visited));
        }

        return false;
    }

    public void resolveInputMapping(File mappingFile) {
        try (Reader reader = new FileReader(mappingFile)) {
            JsonObject resolved = (JsonObject) JsonParser.parseReader(reader);
            if (resolved.has("version")) {
                if (MAPPING_VERSION.equals(resolved.getAsJsonPrimitive("version").getAsString())) {
                    JsonObject packages = resolved.getAsJsonObject("packages");
                    if (packages != null) {
                        for (Map.Entry<String, JsonElement> entry : packages.asMap().entrySet()) {
                            String name = entry.getKey();
                            JsonObject object = entry.getValue().getAsJsonObject();
                            if (object.has("obfuscated"))
                                packageMappings.put(name, object.getAsJsonPrimitive("obfuscated").getAsString());
                            if (object.has("unique_index")) {
                                Dictionary dictionary = obfuscator.packageDictionaries.computeIfAbsent(name, key -> obfuscator.getDictionary().copy());
                                dictionary.setUniqueIndex(object.getAsJsonPrimitive("unique_index").getAsInt());
                            }
                            if (object.has("class_unique_index")) {
                                Dictionary dictionary = obfuscator.classesDictionaries.computeIfAbsent(name, key -> obfuscator.getDictionary().copy());
                                dictionary.setUniqueIndex(object.getAsJsonPrimitive("class_unique_index").getAsInt());
                            }
                        }
                    }

                    JsonObject classes = resolved.getAsJsonObject("classes");
                    if (classes != null) {
                        for (Map.Entry<String, JsonElement> entry : classes.asMap().entrySet()) {
                            String name = entry.getKey();
                            JsonObject object = entry.getValue().getAsJsonObject();
                            try {
                                // compute dummy mappings and give methods and fields unique seeds index
                                ClassTree tree = obfuscator.getTree(name);
                                Map<String, String> mapped = new HashMap<>();

                                if (object.has("methods")) {
                                    JsonObject methods = object.getAsJsonObject("methods");
                                    for (Map.Entry<String, JsonElement> methodEntry : methods.asMap().entrySet()) {
                                        final String methodName = methodEntry.getKey();
                                        final String obfuscated = methodEntry.getValue().getAsJsonPrimitive().getAsString();
                                        methodMappings.put(name + "." + methodName, obfuscated);
                                        mapped.put(methodName, obfuscated);
                                    }
                                }
                                if (object.has("fields")) {
                                    JsonObject methods = object.getAsJsonObject("fields");
                                    for (Map.Entry<String, JsonElement> fieldEntry : methods.asMap().entrySet()) {
                                        final String fieldName = fieldEntry.getKey();
                                        final String obfuscated = fieldEntry.getValue().getAsJsonPrimitive().getAsString();
                                        fieldMappings.put(name + "." + fieldName, obfuscated);
                                        mapped.put(fieldName, obfuscated);
                                    }
                                }
                                generateDummy(name, mapped, new HashSet<>());

                                ClassWrapper classWrapper = tree.getClassWrapper();
                                if (object.has("method_unique_index")) {
                                    classWrapper.getMethodDictionary().setUniqueIndex(object.getAsJsonPrimitive("method_unique_index").getAsInt());
                                }
                                if (object.has("field_unique_index")) {
                                    classWrapper.getFieldDictionary().setUniqueIndex(object.getAsJsonPrimitive("field_unique_index").getAsInt());
                                }
                            } catch (RuntimeException e) {
                                ERROR(TRANSLATION("phantom-shield-x.mapper.cant"), name);
                            }

                            if (object.has("obfuscated")) {
                                classMappings.put(name, object.getAsJsonPrimitive("obfuscated").getAsString());
                            }
                        }
                    }
                    JsonObject annotations = resolved.getAsJsonObject("annotations");
                    if (annotations != null) {
                        for (Map.Entry<String, JsonElement> entry : annotations.asMap().entrySet()) {
                            String name = entry.getKey();
                            JsonObject object = entry.getValue().getAsJsonObject();
                            if (object.has("values")) {
                                JsonObject values = object.getAsJsonObject("values");
                                for (Map.Entry<String, JsonElement> valueEntry : values.asMap().entrySet()) {
                                    annotationMappings.put(name + "." + valueEntry.getKey(), valueEntry.getValue().getAsJsonPrimitive().getAsString());
                                }
                            }
                        }
                    }
                } else {
                    throw new RuntimeException("mappings version is mismatch: " + MAPPING_VERSION);
                }
            } else {
                throw new RuntimeException("is not a valid phantom-shield-x mappings file");
            }
        } catch (FileNotFoundException fnfe) {
            ERROR(TRANSLATION("phantom-shield-x.mapper.cant2"), fnfe);
        } catch (IOException e) {
            ERROR(TRANSLATION("phantom-shield-x.mapper.occurs"), e);
        } catch (RuntimeException e) {
            ERROR("", e);
        }

    }

    public void printMappings(File mappingFile) {

        if (mappingFile.exists()) FileUtils.renameExistingFile(mappingFile);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(mappingFile));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final JsonObject mappings = new JsonObject();
            mappings.addProperty("version", MAPPING_VERSION);

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
                if (parts.length != 2) throw new RuntimeException("impossible annotation mapping: " + origin);
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
                if (parts.length != 2) throw new RuntimeException("impossible method mapping: " + origin);
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
                if (parts.length != 3) throw new RuntimeException("impossible method mapping: " + origin);
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
            gson.toJson(mappings, bw);
            bw.close();
        } catch (Throwable t) {
            ERROR(TRANSLATION("phantom-shield-x.mapper.ran"), t);
        }
    }

    private void generateDummy(String ref, Map<String, String> mapped, Set<String> visited) {
        if (!visited.add(ref))
            return;
        ClassTree tree = obfuscator.getTree(ref);
        for (String className : tree.getSubClasses()) {
            for (Map.Entry<String, String> entry : mapped.entrySet()) {
                String origin = entry.getKey();
                String obfuscated = entry.getValue();
                dummy.computeIfAbsent(className + "." + origin, k -> obfuscated);
                generateDummy(className, mapped, visited);
            }
        }
        for (String className : tree.getParentClasses()) {
            for (Map.Entry<String, String> entry : mapped.entrySet()) {
                String origin = entry.getKey();
                String obfuscated = entry.getValue();
                dummy.computeIfAbsent(className + "." + origin, k -> obfuscated);
                generateDummy(className, mapped, visited);
            }
        }
    }

    private static boolean methodCanBeRenamed(MethodWrapper wrapper) {
        return !wrapper.getAccess().isNative() && !"main".equals(wrapper.getOriginalName()) && !"premain".equals(wrapper.getOriginalName()) && !wrapper.getOriginalName().startsWith("<");
    }

    public Map<String, String> getMethodMappings() {
        return methodMappings;
    }

    public Map<String, String> getFieldMappings() {
        return fieldMappings;
    }

    public Map<String, String> getClassMappings() {
        return classMappings;
    }

    public Map<String, String> getPackageMappings() {
        return packageMappings;
    }

    public Map<String, String> getAnnotationMappings() {
        return annotationMappings;
    }

    public Map<String, String> getMappings() {
        return mappings;
    }

    public void setPrefixName(String prefix_name) {
        this.prefix_name = prefix_name;
    }

    public void setRepackage(boolean repackage) {
        this.repackage = repackage;
    }

    public void setRepakageName(String repakage_name) {
        this.repakage_name = repakage_name;
    }

    public void setRandomPackage(boolean random_package) {
        this.random_package = random_package;
    }
}
