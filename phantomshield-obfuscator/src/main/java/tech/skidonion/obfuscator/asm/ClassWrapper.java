package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.accesses.ClassAccess;
import tech.skidonion.obfuscator.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

/**
 * Wrapper for ClassNodes.
 */
public class ClassWrapper {
    private static final int LIB_FLAGS = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE;
    private static final int INPUT_FLAGS = ClassReader.SKIP_FRAMES;
    private static final String DEFAULT_ENTRY_PREFIX = "";

    private final PhantomShield obfuscator;
    private ClassNode classNode;
    private final String originalName;
    private final String originalSuperName;
    private final boolean libraryNode;

    private String entryPrefix;
    private final Access access;
    private Dictionary methodDictionary;
    private Dictionary fieldDictionary;
    private final List<AnnotationNode> originalAnnotations = new ArrayList<>();
    private final List<String> originalInterfaces = new ArrayList<>();
    private final List<MethodWrapper> methods = new ArrayList<>();
    private final List<FieldWrapper> fields = new ArrayList<>();
    private final List<String> strConsts = new ArrayList<>();
    private final Set<String> methodNames = new HashSet<>();
    private final Set<String> methodDescriptors = new HashSet<>();
    private final Set<String> fieldNames = new HashSet<>();
    private final Set<String> fieldDescriptors = new HashSet<>();

    public ClassWrapper(PhantomShield obfuscator, ClassReader cr, boolean libraryNode) {
        this.obfuscator = obfuscator;
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, libraryNode ? LIB_FLAGS : INPUT_FLAGS);

        this.classNode = classNode;
        this.originalName = classNode.name;
        this.originalSuperName = classNode.superName;
        this.libraryNode = libraryNode;

        this.entryPrefix = DEFAULT_ENTRY_PREFIX;
        this.access = new ClassAccess(this);

        if (classNode.visibleAnnotations != null) {
            originalAnnotations.addAll(classNode.visibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (classNode.invisibleAnnotations != null) {
            originalAnnotations.addAll(classNode.invisibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (classNode.interfaces != null) {
            originalInterfaces.addAll(classNode.interfaces.stream().map(String::new).collect(Collectors.toList()));
        }

        classNode.methods.forEach(methodNode -> {
            methodNames.add(methodNode.name);
            methodDescriptors.add(methodNode.name + methodNode.desc);
            methods.add(new MethodWrapper(methodNode, this));
        });
        classNode.fields.forEach(fieldNode -> {
            fieldNames.add(fieldNode.name);
            fieldDescriptors.add(fieldNode.name + "." + fieldNode.desc);
            fields.add(new FieldWrapper(fieldNode, this));
        });
    }

    public ClassWrapper(PhantomShield obfuscator, ClassNode classNode, boolean libraryNode) {
        this.obfuscator = obfuscator;
        this.classNode = classNode;
        this.originalName = classNode.name;
        this.originalSuperName = classNode.superName;
        this.libraryNode = libraryNode;

        this.entryPrefix = DEFAULT_ENTRY_PREFIX;
        this.access = new ClassAccess(this);

        if (classNode.visibleAnnotations != null) {
            originalAnnotations.addAll(classNode.visibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (classNode.invisibleAnnotations != null) {
            originalAnnotations.addAll(classNode.invisibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (classNode.interfaces != null) {
            originalInterfaces.addAll(classNode.interfaces.stream().map(String::new).collect(Collectors.toList()));
        }
        classNode.methods.forEach(methodNode -> {
            methodNames.add(methodNode.name);
            methodDescriptors.add(methodNode.name + methodNode.desc);
            methods.add(new MethodWrapper(methodNode, this));
        });
        classNode.fields.forEach(fieldNode -> {
            fieldNames.add(fieldNode.name);
            fieldDescriptors.add(fieldNode.name + "." + fieldNode.desc);
            fields.add(new FieldWrapper(fieldNode, this));
        });
    }

    public void addMethod(MethodNode methodNode) {
        methodNames.add(methodNode.name);
        methodDescriptors.add(methodNode.name + methodNode.desc);
        classNode.methods.add(methodNode);
        methods.add(new MethodWrapper(methodNode, this));
    }

    public void addMethod(MethodWrapper methodWrapper) {
        methodNames.add(methodWrapper.getName());
        methodDescriptors.add(methodWrapper.getName() + methodWrapper.getDescription());
        classNode.methods.add(methodWrapper.getMethodNode());
        methods.add(methodWrapper);
    }

    public void addField(FieldNode fieldNode) {
        fieldNames.add(fieldNode.name);
        fieldDescriptors.add(fieldNode.name + "." + fieldNode.desc);
        classNode.fields.add(fieldNode);
        fields.add(new FieldWrapper(fieldNode, this));
    }

    public void addField(FieldWrapper fieldWrapper) {
        fieldNames.add(fieldWrapper.getName());
        fieldDescriptors.add(fieldWrapper.getName() + "." + fieldWrapper.getDescription());
        classNode.fields.add(fieldWrapper.getFieldNode());
        fields.add(fieldWrapper);
    }

    public void updateMemberNames() {
        this.fieldNames.clear();
        this.methodNames.clear();
        this.fieldDescriptors.clear();
        this.methodDescriptors.clear();
        fields.forEach(field -> {
            this.fieldNames.add(field.getName());
            this.fieldDescriptors.add(field.getName() + "." + field.getDescription());
        });
        methods.forEach(method -> {
            this.methodNames.add(method.getName());
            this.methodDescriptors.add(method.getName() + method.getDescription());
        });
    }

    /**
     * @param s constant literal to add to constant pool.
     */
    public void addStringConst(String s) {
        strConsts.add(s);
    }

    public MethodNode getMethod(String name, String desc) {
        return getClassNode().methods.stream().filter(methodNode -> name.equals(methodNode.name)
                && desc.equals(methodNode.desc)).findAny().orElse(null);
    }

    public FieldNode getField(String name, String desc) {
        return getClassNode().fields.stream().filter(methodNode -> name.equals(methodNode.name)
                && desc.equals(methodNode.desc)).findAny().orElse(null);
    }


    public MethodNode getOrCreateClinit() {
        MethodNode clinit = getMethod("<clinit>", "()V");
        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            addMethod(clinit);
        }
        return clinit;
    }

    MethodNode dummy;

    public MethodNode getOrCreateDummyMethod() {
        if (dummy == null) {
            dummy = new MethodNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, PhantomShield.initMethodName, "()V", null, null);
            dummy.instructions.add(new InsnNode(Opcodes.RETURN));
            addMethod(dummy);
        }

        return dummy;
    }

    public boolean isMethodPresent(String name, String desc) {
        return classNode.methods.stream().anyMatch(methodNode -> methodNode.name.equals(name) && methodNode.desc.equals(desc));
    }

    public boolean isFieldPresent(String name, String desc) {
        return classNode.fields.stream().anyMatch(fieldNode -> fieldNode.name.equals(name) && fieldNode.desc.equals(desc));
    }

    /**
     * Attached class node.
     */
    public ClassNode getClassNode() {
        return classNode;
    }

    public void setClassNode(ClassNode classNode) {
        this.classNode = classNode;
    }

    /**
     * @return original name of wrapped {@link ClassNode}.
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @return true if this wrapper represents a library class.
     */
    public boolean isLibraryNode() {
        return libraryNode;
    }

    /**
     * @return {@link ArrayList} of {@link MethodWrapper}s this wrapper contains.
     */
    public List<MethodWrapper> getMethods() {
        return methods;
    }

    /**
     * @return {@link ArrayList} of {@link FieldWrapper}s this wrapper contains.
     */
    public List<FieldWrapper> getFields() {
        return fields;
    }

    public List<String> getStrConsts() {
        return strConsts;
    }

    /**
     * @return current name of wrapped {@link ClassNode}.
     */
    public String getName() {
        return classNode.name;
    }

    /**
     * @return current package name of wrapped {@link ClassNode}.
     */
    public String getPackageName() {
        return classNode.name.substring(0, classNode.name.lastIndexOf('/') + 1);
    }

    public String getOriginPackageName() {
        return getOriginalName().substring(0, getOriginalName().lastIndexOf('/') + 1);
    }

    /**
     * @return current super class name of wrapped {@link ClassNode}.
     */
    public String getSuperName() {
        return classNode.superName;
    }

    /**
     * @return current interfaces of wrapped {@link ClassNode}.
     */
    public List<String> getInterfaces() {
        return classNode.interfaces;
    }

    /**
     * @return {@link ClassAccess} wrapper of represented {@link ClassNode}'s access flags.
     */
    public Access getAccess() {
        return access;
    }

    /**
     * @return raw access flags of wrapped {@link ClassNode}.
     */
    public int getAccessFlags() {
        return classNode.access;
    }

    /**
     * @param access access flags to set.
     */
    public void setAccessFlags(int access) {
        classNode.access = access;
    }

    /**
     * @return the current class version of the wrapped {@link ClassNode}.
     */
    public int getVersion() {
        return classNode.version;
    }

    /**
     * See https://docs.oracle.com/javase/specs/jvms/se12/html/jvms-4.html#jvms-4.9.1
     *
     * @return true if the wrapped {@link ClassNode} supports JSR instructions.
     */
    public boolean allowsJSR() {
        return classNode.version <= Opcodes.V1_5 || classNode.version == Opcodes.V1_1;
    }

    /**
     * J7 and up include support for INVOKEDYNAMIC instructions.
     *
     * @return true if the wrapped {@link ClassNode} supports INVOKEDYNAMIC instructions.
     */
    public boolean allowsIndy() {
        return classNode.version >= Opcodes.V1_7 && classNode.version != Opcodes.V1_1;
    }

    public boolean allowsDynamicConstant() {
        return classNode.version >= Opcodes.V11 && classNode.version != Opcodes.V1_1;
    }

    /**
     * @return the computed current constant pool size of the wrapped {@link ClassNode}.
     */
    public int computeConstantPoolSize() {
        return new ClassReader(toByteArray()).getItemCount();
    }

    public String getOriginalSuperName() {
        return originalSuperName;
    }

    public List<AnnotationNode> getOriginalAnnotations() {
        return originalAnnotations;
    }

    public List<String> getOriginalInterfaces() {
        return originalInterfaces;
    }

    public byte[] toByteArray() {
        // Construct byte writer
        ClassWriter writer = new CustomClassWriter(allowsJSR() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES, obfuscator);

        try {
            writer.newUTF8("PHANTOMSHIELD" + PhantomShield.VERSION);
            // Populate writer with class info
            classNode.accept(writer);

            // Insert manually-specified constant pool strings
            strConsts.forEach(writer::newUTF8);

            return writer.toByteArray();
        } catch (Throwable t) {
            INFO(TRANSLATION("phantom-shield-x.class-wrapper.error"), getName() + ".class");
            t.printStackTrace();

            writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            writer.newUTF8("PHANTOMSHIELD" + PhantomShield.VERSION);

            classNode.accept(writer);
            strConsts.forEach(writer::newUTF8);

            return writer.toByteArray();
        }
    }

    public void setEntryPrefix(String entryPrefix) {
        this.entryPrefix = entryPrefix;
    }

    public String getEntryName() {
        return entryPrefix + classNode.name + ".class";
    }

    public Dictionary getFieldDictionary() {
        if (fieldDictionary == null)
            fieldDictionary = obfuscator.getDictionary().copy();
        return fieldDictionary;
    }

    public Dictionary getMethodDictionary() {
        if (methodDictionary == null)
            methodDictionary = obfuscator.getDictionary().copy();
        return methodDictionary;
    }

    public String generateRandomStaticMethodName() {
        String generated;
        do {
            generated = this.getMethodDictionary().nextUniqueString();
        } while (!isStaticMethodNameUnique(generated, getOriginalName()));
        return generated;
    }

    private boolean isStaticMethodNameUnique(String name, String owner) {
        ClassTree tree = obfuscator.getTree(owner);
        return !tree.getClassWrapper().methodNames.contains(name);
    }

    public String generateRandomMethodName() {
        String generated;
        Set<String> visited;
        do {
            visited = new HashSet<>();
            generated = this.getMethodDictionary().nextUniqueString();
        } while (!isMethodNameUnique(generated, getOriginalName(), visited));
        return generated;
    }


    public String generateRandomStaticFieldName() {
        String generated;
        do {
            generated = this.getFieldDictionary().nextUniqueString();
        } while (!isStaticFieldNameUnique(generated, getOriginalName()));
        return generated;
    }

    private boolean isStaticFieldNameUnique(String name, String owner) {
        ClassTree tree = obfuscator.getTree(owner);
        return !tree.getClassWrapper().fieldNames.contains(name);
    }

    public String generateRandomFieldName() {
        String generated;
        Set<String> visited;
        do {
            visited = new HashSet<>();
            generated = this.getFieldDictionary().nextUniqueString();
        } while (!isFieldNameUnique(generated, getOriginalName(), visited));
        return generated;
    }

    private boolean isFieldNameUnique(String name, String owner, Set<String> visited) {
        if (visited.contains(owner))
            return true;
        visited.add(owner);
        ClassTree tree = obfuscator.getTree(owner);
        if (tree.getClassWrapper().fieldNames.contains(name))
            return false;
        for (String parent : tree.getParentClasses()) {
            boolean flag = isFieldNameUnique(name, parent, visited);
            if (!flag) return false;
        }
        for (String sub : tree.getSubClasses()) {
            boolean flag = isFieldNameUnique(name, sub, visited);
            if (!flag) return false;
        }
        return true;
    }

    private boolean isMethodNameUnique(String name, String owner, Set<String> visited) {
        if (visited.contains(owner))
            return true;
        visited.add(owner);
        ClassTree tree = obfuscator.getTree(owner);
        if (tree.getClassWrapper().methodNames.contains(name))
            return false;
        for (String parent : tree.getParentClasses()) {
            boolean flag = isMethodNameUnique(name, parent, visited);
            if (!flag) return false;
        }
        for (String sub : tree.getSubClasses()) {
            boolean flag = isMethodNameUnique(name, sub, visited);
            if (!flag) return false;
        }
        return true;
    }

    public Set<String> getMethodNames() {
        return methodNames;
    }

    public Set<String> getFieldNames() {
        return fieldNames;
    }

    public Set<String> getMethodDescriptors() {
        return methodDescriptors;
    }

    public Set<String> getFieldDescriptors() {
        return fieldDescriptors;
    }
}
