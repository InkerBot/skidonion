package tech.skidonion.obfuscator.asm;

import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import tech.skidonion.obfuscator.asm.accesses.Access;
import tech.skidonion.obfuscator.asm.accesses.MethodAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wrapper for MethodNodes.
 */
public class MethodWrapper {
    // https://docs.oracle.com/javase/specs/jvms/se12/html/jvms-4.html#jvms-4.7.3
    private static final int MAX_CODE_SIZE = 65535;

    private MethodNode methodNode;
    private final String originalName;
    private final String originalDescription;
    private final List<AnnotationNode> originalAnnotations = new ArrayList<>();

    private final Access access;
    private final ClassWrapper owner;

    /**
     * Creates a MethodWrapper object.
     *
     * @param methodNode the {@link MethodNode} this wrapper represents.
     * @param owner      the owner of this represented method.
     */
    public MethodWrapper(MethodNode methodNode, ClassWrapper owner) {
        this.methodNode = methodNode;
        this.originalName = methodNode.name;
        this.originalDescription = methodNode.desc;

        if (methodNode.visibleAnnotations != null) {
            originalAnnotations.addAll(methodNode.visibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }
        if (methodNode.invisibleAnnotations != null) {
            originalAnnotations.addAll(methodNode.invisibleAnnotations.stream().map(annotationNode -> new AnnotationNode(annotationNode.desc)).collect(Collectors.toList()));
        }

        this.access = new MethodAccess(this);
        this.owner = owner;
    }

    /**
     * Attached MethodNode.
     */
    public MethodNode getMethodNode() {
        return methodNode;
    }

    public void setMethodNode(MethodNode methodNode) {
        this.methodNode = methodNode;
    }

    /**
     * @return owner of this wrapper.
     */
    public ClassWrapper getOwner() {
        return owner;
    }

    /**
     * @return original name of wrapped {@link MethodNode}.
     */
    public String getOriginalName() {
        return originalName;
    }

    /**
     * @return original description of wrapped {@link MethodNode}
     */
    public String getOriginalDescription() {
        return originalDescription;
    }

    /**
     * @return the current name of wrapped {@link MethodNode}.
     */
    public String getName() {
        return methodNode.name;
    }

    /**
     * @return the current description of wrapped {@link MethodNode}.
     */
    public String getDescription() {
        return methodNode.desc;
    }

    /**
     * @return the current {@link InsnList} of wrapped {@link MethodNode}.
     */
    public InsnList getInstructions() {
        return methodNode.instructions;
    }

    public void setInstructions(InsnList instructions) {
        methodNode.instructions = instructions;
    }

    /**
     * @return the current {@link TryCatchBlockNode}s of wrapped {@link MethodNode}.
     */
    public List<TryCatchBlockNode> getTryCatchBlocks() {
        return methodNode.tryCatchBlocks;
    }

    /**
     * @return {@link MethodAccess} wrapper of represented {@link MethodNode}'s access flags.
     */
    public Access getAccess() {
        return access;
    }

    /**
     * @return raw access flags of wrapped {@link MethodNode}.
     */
    public int getAccessFlags() {
        return methodNode.access;
    }

    /**
     * @param access access flags to set.
     */
    public void setAccessFlags(int access) {
        methodNode.access = access;
    }

    /**
     * @return the current max allocation of local variables (registers) of wrapped {@link MethodNode}.
     */
    public int getMaxLocals() {
        return methodNode.maxLocals;
    }

    public void setMaxLocals(int maxLocals) {
        methodNode.maxLocals = maxLocals;
    }

    /**
     * @return true if the wrapped {@link MethodNode} represented by this wrapper contains instructions.
     */
    public boolean hasInstructions() {
        return methodNode.instructions != null && methodNode.instructions.size() > 0;
    }

    /**
     * @return computes and returns the size of the wrapped {@link MethodNode}.
     */
    public int getCodeSize() {
        CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
        methodNode.accept(cse);
        return cse.getMaxSize();
    }

    /**
     * @return the leeway between the current size of the wrapped {@link MethodNode} and the max allowed size.
     */
    public int getLeewaySize() {
        return MAX_CODE_SIZE - getCodeSize();
    }

    public List<AnnotationNode> getOriginalAnnotations() {
        return originalAnnotations;
    }
}
