package tech.skidonion.obfuscator.transformer;

import lombok.Getter;
import lombok.Setter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.annotations.verification.LoadAfterLogin;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.value.Value;

import java.util.*;
import java.util.stream.Stream;

import static org.objectweb.asm.ClassReader.SKIP_FRAMES;

@LoadAfterLogin(value = "基础用户组", priority = 0)
public abstract class Transformer implements Opcodes {
    public PhantomShield obfuscator;
    private final boolean forceEnabled;
    @Getter
    private final String name;
    @Setter
    private boolean enabled;
    @Setter
    private Filter filter;
    private final List<Value<?>> settings = new ArrayList<>();
    private final Map<String, Set<String>> internal = new HashMap<>();

    public Transformer(String name) {
        this(name, false);
    }

    public Transformer(String name, boolean forceEnable) {
        this.name = name;
        this.enabled = false;
        this.forceEnabled = forceEnable;
    }

    public final void init(PhantomShield obfuscator) {
        this.obfuscator = obfuscator;
    }

    public abstract void preprocess() throws Exception;

    public abstract void transform() throws Exception;

    public abstract void postprocess() throws Exception;

    /*
     * nullable
     * */
    public abstract String annotation();


    public final ClassWrapper injectClass(ClassNode classNode) {
        return injectClass("", classNode);
    }

    public final ClassWrapper injectClass(String entryPrefix, ClassNode classNode) {
        ClassWrapper cw = new ClassWrapper(obfuscator, classNode, ClassWrapper.ProcessType.INPUT);
        cw.setEntryPrefix(entryPrefix);
        obfuscator.classes.put(cw.getName(), cw);
        obfuscator.classpath.put(cw.getName(), cw);
        return cw;
    }

    public final List<ClassWrapper> injectClasses(Collection<ClassNode> classNodes) {
        return injectClasses("", classNodes);
    }

    public final List<ClassWrapper> injectClasses(String entryPrefix, Collection<ClassNode> classNodes) {
        List<ClassWrapper> val = new ArrayList<>();
        for (ClassNode classNode : classNodes) {
            ClassWrapper cw = new ClassWrapper(obfuscator, classNode, ClassWrapper.ProcessType.INPUT);
            cw.setEntryPrefix(entryPrefix);
            obfuscator.classes.put(cw.getName(), cw);
            obfuscator.classpath.put(cw.getName(), cw);
            val.add(cw);
        }
        return val;
    }

    public final void injectClassAsResource(ClassNode classNode) {
        injectClassAsResource("", classNode);
    }

    public final void injectClassAsResource(String entryPrefix, ClassNode classNode) {
        ClassWriter cw = new ClassWriter(0);
        classNode.accept(cw);
        obfuscator.resources.put(entryPrefix + classNode.name + ".class", cw.toByteArray());
    }

    public final void injectClassesAsResource(Collection<ClassNode> classNodes) {
        injectClassesAsResource("", classNodes);
    }

    public final void injectClassesAsResource(String entryPrefix, Collection<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            obfuscator.resources.put(entryPrefix + classNode.name + ".class", cw.toByteArray());
        }
    }

    public final void injectResources(Map<String, byte[]> resources) {
        injectResources("", resources);
    }

    public final void injectResources(String entryPrefix, Map<String, byte[]> resources) {
        resources.forEach((entryName, data) -> obfuscator.resources.put(entryPrefix + entryName, data));
    }

    protected void addSetting(Value<?> setting) {
        settings.add(setting);
    }

    protected void addSettings(Value<?>... settings) {
        for (Value<?> setting : settings) {
            addSetting(setting);
        }
    }

    public Value<?>[] getSettings() {
        return settings.toArray(new Value[0]);
    }

    public boolean isEnabled() {
        return enabled || forceEnabled;
    }

    public boolean match(String expression) {
        if (filter == null) return true;
        return filter.match(expression);
    }


    public boolean match(MethodWrapper method) {
        if (internalMatch(method))
            return true;
        if (hasAnnotation(method)) return matchAnnotation(method);
        if (filter == null) return true;
        return filter.match(method);
    }

    public boolean match(FieldWrapper field) {
        if (internalMatch(field))
            return true;
        if (hasAnnotation(field)) return matchAnnotation(field);
        if (filter == null) return true;
        return filter.match(field);
    }

    public boolean match(ClassWrapper clazz) {
        if (internalMatch(clazz)) return true;
        if (hasAnnotation(clazz)) return matchAnnotation(clazz);
        if (filter == null) return true;
        return filter.match(clazz);
    }

    public boolean internalMatch(MethodWrapper method) {
        Set<String> internals;
        return (internals = this.internal.get(method.getOwner().getOriginalName())) != null && (internals.contains("*") || internals.contains(method.getOriginalName() + method.getOriginalDescription()));
    }

    public boolean internalMatch(FieldWrapper field) {
        Set<String> internals = this.internal.get(field.getOwner().getOriginalName());
        return internals != null && (internals.contains("*") || internals.contains(field.getOriginalName() + "." + field.getOriginalDescription()));
    }

    public boolean internalMatch(ClassWrapper clazz) {
        return this.internal.containsKey(clazz.getOriginalName());
    }

    public void addInternalInclusion(String owner, String descriptor) {
        this.internal.compute(owner, (key, value) -> {
            if (value == null) {
                value = new HashSet<>();
            }
            value.add(descriptor);
            return value;
        });
    }

    public void addInternalInclusions(String owner, String... descriptor) {
        this.internal.compute(owner, (key, value) -> {
            if (value == null) {
                value = new HashSet<>();
            }
            value.addAll(Arrays.asList(descriptor));
            return value;
        });
    }

    public final Stream<ClassWrapper> getFilteredClasses() {
        return getClassWrappers().stream().filter(this::match);
    }

    protected final Map<String, ClassWrapper> getClasses() {
        return this.obfuscator.classes;
    }

    protected final Collection<ClassWrapper> getClassWrappers() {
        return this.obfuscator.classes.values();
    }

    protected final Collection<ClassWrapper> getSoftExcludedClasses() {
        return this.obfuscator.softExclusions.values();
    }

    protected final Map<String, ClassWrapper> getClassPath() {
        return this.obfuscator.classpath;
    }

    protected final Map<String, byte[]> getResources() {
        return this.obfuscator.resources;
    }

    public final boolean hasAnnotation(ClassWrapper classWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(classWrapper, annotation());
    }

    public final boolean hasAnnotation(MethodWrapper methodWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(methodWrapper, annotation());
    }

    public final boolean hasAnnotation(FieldWrapper fieldWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(fieldWrapper, annotation());
    }

    public final Map<String, Object> getAnnotationValues(ClassWrapper classWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(classWrapper, annotation());
    }

    public final Map<String, Object> getAnnotationValues(MethodWrapper methodWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(methodWrapper, annotation());
    }

    public final Map<String, Object> getAnnotationValues(FieldWrapper fieldWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(fieldWrapper, annotation());
    }

    public final void removeAnnotation(ClassWrapper classWrapper) {
        if (annotation() == null)
            return;
        ASMUtils.removeAnnotation(classWrapper, annotation());
    }

    public final void removeAnnotation(FieldWrapper fieldWrapper) {
        if (annotation() == null)
            return;
        ASMUtils.removeAnnotation(fieldWrapper, annotation());
    }

    public final void removeAnnotation(MethodWrapper methodWrapper) {
        if (annotation() == null)
            return;
        ASMUtils.removeAnnotation(methodWrapper, annotation());
    }

    public final boolean matchAnnotation(ClassWrapper classWrapper) {
        Map<String, Object> map = getAnnotationValues(classWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true").toString());
    }

    public final boolean matchAnnotation(MethodWrapper methodWrapper) {
        Map<String, Object> map = getAnnotationValues(methodWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true").toString());
    }

    public final boolean matchAnnotation(FieldWrapper fieldWrapper) {
        Map<String, Object> map = getAnnotationValues(fieldWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true").toString());
    }


}
