package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.accesses.ClassAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static tech.skidonion.obfuscator.PhantomShield.INFO;

/**
 * Wrapper for ClassNodes.
 */
public class ClassWrapper {
    private static final int LIB_FLAGS = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES | ClassReader.SKIP_CODE;
    private static final int INPUT_FLAGS = ClassReader.SKIP_FRAMES;
    private static final String DEFAULT_ENTRY_PREFIX = "";

    private ClassNode classNode;
    private final String originalName;
    private final String originalSuperName;
    private final boolean libraryNode;

    private String entryPrefix;
    private final Access access;
    private final List<AnnotationNode> originalAnnotations = new ArrayList<>();
    private final List<String> originalInterfaces = new ArrayList<>();
    private final List<MethodWrapper> methods = new ArrayList<>();
    private final List<FieldWrapper> fields = new ArrayList<>();
    private final List<String> strConsts = new ArrayList<>();


    public ClassWrapper(ClassReader cr, boolean libraryNode) {
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

        classNode.methods.forEach(methodNode -> methods.add(new MethodWrapper(methodNode, this)));
        classNode.fields.forEach(fieldNode -> fields.add(new FieldWrapper(fieldNode, this)));
    }

    public ClassWrapper(ClassNode classNode, boolean libraryNode) {
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
        classNode.methods.forEach(methodNode -> methods.add(new MethodWrapper(methodNode, this)));
        classNode.fields.forEach(fieldNode -> fields.add(new FieldWrapper(fieldNode, this)));
    }

    public void addMethod(MethodNode methodNode) {
        classNode.methods.add(methodNode);
        methods.add(new MethodWrapper(methodNode, this));
    }

    public void addField(FieldNode fieldNode) {
        classNode.fields.add(fieldNode);
        fields.add(new FieldWrapper(fieldNode, this));
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

    public MethodNode getOrCreateClinit() {
        MethodNode clinit = getMethod("<clinit>", "()V");

        if (clinit == null) {
            clinit = new MethodNode(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null);
            clinit.instructions.add(new InsnNode(Opcodes.RETURN));
            addMethod(clinit);
        }

        return clinit;
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

    /**
     * @return the computed current constant pool size of the wrapped {@link ClassNode}.
     */
    public int computeConstantPoolSize(PhantomShield obfuscator) {
        return new ClassReader(toByteArray(obfuscator)).getItemCount();
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

    public byte[] toByteArray(PhantomShield obfuscator) {
        // Construct byte writer
        ClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_FRAMES, obfuscator);

        try {
            writer.newUTF8("PHANTOMSHIELD" + PhantomShield.VERSION);

            // Populate writer with class info
            classNode.accept(writer);

            // Insert manually-specified constant pool strings
            strConsts.forEach(writer::newUTF8);

            return writer.toByteArray();
        } catch (Throwable t) {
            INFO("Error writing class {}. Skipping frames (might cause runtime errors).", getName() + ".class");
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
}
