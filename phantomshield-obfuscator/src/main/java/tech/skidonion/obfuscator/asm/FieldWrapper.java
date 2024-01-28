package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FieldNode;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.accesses.FieldAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper for FieldNodes.
 */
public class FieldWrapper {
    private FieldNode fieldNode;
    private final String originalName;
    private final String originalDescription;
    private final List<AnnotationNode> originalAnnotations = new ArrayList<>();
    private final Access access;
    private final ClassWrapper owner;

    /**
     * Creates a FieldWrapper object.
     *
     * @param fieldNode the {@link FieldNode} attached to this FieldWrapper.
     * @param owner     the owner of this represented field.
     */
    public FieldWrapper(FieldNode fieldNode, ClassWrapper owner) {
        this.fieldNode = fieldNode;
        this.originalName = fieldNode.name;
        this.originalDescription = fieldNode.desc;

        if (fieldNode.visibleAnnotations != null) {
            originalAnnotations.addAll(fieldNode.visibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (fieldNode.invisibleAnnotations != null) {
            originalAnnotations.addAll(fieldNode.invisibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }

        this.access = new FieldAccess(this);
        this.owner = owner;
    }

    /**
     * @return wrapped {@link FieldNode}.
     */
    public FieldNode getFieldNode() {
        return fieldNode;
    }

    public void setFieldNode(FieldNode fieldNode) {
        this.fieldNode = fieldNode;
    }

    /**
     * @return owner of this wrapper.
     */
    public ClassWrapper getOwner() {
        return owner;
    }

    /**
     * @return original name of wrapped {@link FieldNode}.
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @return original description of wrapped {@link FieldNode}
     */
    public String getOriginalDescription() {
        return originalDescription;
    }

    /**
     * @return the current name of the wrapped {@link FieldNode}.
     */
    public String getName() {
        return fieldNode.name;
    }

    /**
     * @return the current description of the wrapped {@link FieldNode}.
     */
    public String getDescription() {
        return fieldNode.desc;
    }

    /**
     * @return {@link FieldAccess} wrapper of represented {@link FieldNode}'s access flags.
     */
    public Access getAccess() {
        return access;
    }

    /**
     * @return raw access flags of wrapped {@link FieldNode}.
     */
    public int getAccessFlags() {
        return fieldNode.access;
    }

    /**
     * @param access access flags to set.
     */
    public void setAccessFlags(int access) {
        fieldNode.access = access;
    }

    public List<AnnotationNode> getOriginalAnnotations() {
        return originalAnnotations;
    }
}
