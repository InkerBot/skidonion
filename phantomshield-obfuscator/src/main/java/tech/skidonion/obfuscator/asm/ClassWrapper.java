package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.accesses.ClassAccess;
import tech.skidonion.obfuscator.dictionary.Dictionary;

import java.util.*;
import java.util.stream.Collectors;

import static tech.skidonion.obfuscator.PhantomShield.*;

/**
 * Wrapper for ClassNodes.
 */
public class ClassWrapper {
    private static final String DEFAULT_ENTRY_PREFIX = "";

    private final PhantomShield obfuscator;
    private final Integer writeFlag;
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

    private Set<String> membersHierarchy;
    private final Map<String, MethodWrapper> methodDescriptors = new HashMap<>();
    private final Map<String, FieldWrapper> fieldDescriptors = new HashMap<>();

    public ClassWrapper(PhantomShield obfuscator, ClassReader cr, boolean libraryNode, int readFlag, Integer writeFlag) {
        this.writeFlag = writeFlag;

        this.obfuscator = obfuscator;
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, readFlag);

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
            MethodWrapper methodWrapper = new MethodWrapper(methodNode, this);
            methods.add(methodWrapper);
            methodDescriptors.put(methodNode.name + methodNode.desc, methodWrapper);
        });
        classNode.fields.forEach(fieldNode -> {
            FieldWrapper fieldWrapper = new FieldWrapper(fieldNode, this);
            fields.add(fieldWrapper);
            fieldDescriptors.put(fieldNode.name + "." + fieldNode.desc, fieldWrapper);
        });
    }

    public ClassWrapper(PhantomShield obfuscator, ClassNode classNode, boolean libraryNode, Integer writeFlag) {
        this.writeFlag = writeFlag;
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
            MethodWrapper methodWrapper = new MethodWrapper(methodNode, this);
            methods.add(methodWrapper);
            methodDescriptors.put(methodNode.name + methodNode.desc, methodWrapper);
        });
        classNode.fields.forEach(fieldNode -> {
            FieldWrapper fieldWrapper = new FieldWrapper(fieldNode, this);
            fields.add(fieldWrapper);
            fieldDescriptors.put(fieldNode.name + "." + fieldNode.desc, fieldWrapper);
        });
    }

    public void addMethod(MethodNode methodNode) {
        MethodWrapper methodWrapper = new MethodWrapper(methodNode, this);
        methodDescriptors.put(methodNode.name + methodNode.desc, methodWrapper);
        classNode.methods.add(methodNode);
        methods.add(methodWrapper);
    }

    public void addMethod(MethodWrapper methodWrapper) {
        methodDescriptors.put(methodWrapper.getName() + methodWrapper.getDescription(), methodWrapper);
        classNode.methods.add(methodWrapper.getMethodNode());
        methods.add(methodWrapper);
    }

    public void addField(FieldNode fieldNode) {
        FieldWrapper fieldWrapper = new FieldWrapper(fieldNode, this);
        classNode.fields.add(fieldNode);
        fieldDescriptors.put(fieldNode.name + "." + fieldNode.desc, fieldWrapper);
        fields.add(fieldWrapper);
    }

    public void addField(FieldWrapper fieldWrapper) {
        classNode.fields.add(fieldWrapper.getFieldNode());
        fieldDescriptors.put(fieldWrapper.getName() + "." + fieldWrapper.getDescription(), fieldWrapper);
        fields.add(fieldWrapper);
    }

    public void updateMemberNames() {
        this.fieldDescriptors.clear();
        this.methodDescriptors.clear();
        this.membersHierarchy = null;
        fields.forEach(field -> {
            this.fieldDescriptors.put(field.getName() + "." + field.getDescription(), field);
        });
        methods.forEach(method -> {
            this.methodDescriptors.put(method.getName() + method.getDescription(), method);
        });
    }

    /**
     * @param s constant literal to add to constant pool.
     */
    public void addStringConst(String s) {
        strConsts.add(s);
    }

    public MethodNode getMethod(String name, String desc) {
        MethodWrapper wrapper = methodDescriptors.get(name + desc);
        return wrapper == null ? null : wrapper.getMethodNode();
    }

    public FieldNode getField(String name, String desc) {
        FieldWrapper wrapper = fieldDescriptors.get(name + "." + desc);
        return wrapper == null ? null : wrapper.getFieldNode();
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

    private FieldNode dummyField;

    public FieldNode getOrCreateInitDummyField() {
        String target = "$skidonion$" + Math.abs(getOriginalName().hashCode());
        if (dummyField == null && (dummyField = getField(target, "Z")) == null) {
            dummyField = new FieldNode(Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC, target, "Z", null, null);
            addField(dummyField);
        }
        return dummyField;
    }

    public boolean isMethodPresent(String name, String desc) {
        return methodDescriptors.containsKey(name + desc);
    }

    public boolean isFieldPresent(String name, String desc) {
        return fieldDescriptors.containsKey(name + "." + desc);
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
        ClassWriter writer = new CustomClassWriter(writeFlag != null ? writeFlag : allowsJSR() ? ClassWriter.COMPUTE_MAXS : ClassWriter.COMPUTE_FRAMES, obfuscator);

        try {
            writer.newUTF8("PHANTOMSHIELD" + PhantomShield.VERSION);
            // Populate writer with class info
            classNode.accept(writer);

            // Insert manually-specified constant pool strings
            strConsts.forEach(writer::newUTF8);

            return writer.toByteArray();
        } catch (Throwable t) {
            INFO(TRANSLATION("phantom-shield-x.class-wrapper.error"), getName() + ".class");
            ERROR("", t);

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


    public String generateRandomMethodName(String desc) {
        String generated;
        do {
            generated = this.getMethodDictionary().next();
        } while (!isMemberNameUnique(generated + desc));
        return generated;
    }

    public String generateRandomFieldName(String desc) {
        String generated;
        do {
            generated = this.getFieldDictionary().next();
        } while (!isMemberNameUnique(generated + "." + desc));
        return generated;
    }

    private boolean isMemberNameUnique(String ref) {
        return !buildMemberHierarchy(getName()).contains(ref);
    }

    public static final Set<String> JOBJECT_METHOD_SET = new HashSet<String>() {
        {
            add("hashCode()I");
            add("equals(Ljava/lang/Object;)Z");
            add("toString()Ljava/lang/String;");
            add("clone()Ljava/lang/Object;");
            add("finalize()V");
        }
    };

    private Set<String> buildMemberHierarchy(String clazz) {
        if (membersHierarchy == null) {
            ClassTree tree = obfuscator.getTree(clazz);
            Set<String> members = new HashSet<>();

            for (String sub : tree.getSubClasses()) {
                ClassWrapper cw = obfuscator.getClassWrapper(sub);

                members.addAll(cw.fieldDescriptors.keySet());
                members.addAll(cw.methodDescriptors.keySet());
            }

            for (String parent : tree.getParentClasses()) {
                ClassWrapper cw = obfuscator.getClassWrapper(parent);

                members.addAll(cw.fieldDescriptors.keySet());
                members.addAll(cw.methodDescriptors.keySet());
            }

            members.addAll(this.fieldDescriptors.keySet());
            members.addAll(this.methodDescriptors.keySet());

            members.addAll(JOBJECT_METHOD_SET);
            return membersHierarchy = members;
        } else {
            return membersHierarchy;
        }
    }

    public Map<String, MethodWrapper> getMethodDescriptors() {
        return methodDescriptors;
    }

    public Map<String, FieldWrapper> getFieldDescriptors() {
        return fieldDescriptors;
    }

}
