package tech.skidonion.obfuscator.transformer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.value.Value;

import java.util.*;
import java.util.stream.Stream;

public abstract class Transformer implements Opcodes {
    protected PhantomShield obfuscator;
    private final boolean forceEnabled;
    private final String name;
    private boolean enabled;
    private Filter filter;
    private final List<Value<?>> settings = new ArrayList<>();

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

    public abstract void transform() throws Exception;

    public abstract void preprocess() throws Exception;

    /*
     * nullable
     * */
    public abstract String annotation();

    protected final void injectClasses(Collection<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            ClassWrapper cw = new ClassWrapper(obfuscator, classNode, false);
            obfuscator.classes.put(cw.getName(), cw);
        }
    }

    protected final void injectClassesAsResource(Collection<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            obfuscator.resources.put(classNode.name + ".class", cw.toByteArray());
        }
    }

    protected final void injectResources(Map<String, byte[]> resources) {
        obfuscator.resources.putAll(resources);
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

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled || forceEnabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    protected boolean match(String expression) {
        if (filter == null) return true;
        return filter.match(expression);
    }

    protected boolean match(MethodWrapper method) {
        if (hasAnnotation(method)) return matchAnnotation(method);
        if (filter == null) return true;
        return filter.match(method);
    }

    protected boolean match(FieldWrapper field) {
        if (hasAnnotation(field)) return matchAnnotation(field);
        if (filter == null) return true;
        return filter.match(field);
    }

    protected boolean match(ClassWrapper clazz) {
        if (hasAnnotation(clazz)) return matchAnnotation(clazz);
        if (filter == null) return true;
        return filter.match(clazz);
    }

    protected final Stream<ClassWrapper> getFilteredClasses() {
        return getClassWrappers().stream().filter(this::match);
    }

    public final void include(String expression) {
        if (filter == null) return;
        if (Objects.requireNonNull(expression).startsWith("-"))
            throw new RuntimeException("Expression Must be a Include Type");
        else if (!expression.startsWith("+")) expression += "+";
        filter.accept(expression);
    }

    public final void include(ClassWrapper cw) {
        if (filter == null) return;
        StringBuilder sb = new StringBuilder("+");

        if (cw.getOriginalAnnotations() != null)
            for (AnnotationNode s : cw.getOriginalAnnotations())
                sb.append('@').append(s.desc, 1, s.desc.length() - 1).append(' ');
        if (cw.getOriginalName() != null) sb.append(cw.getOriginalName());
        if (cw.getOriginalSuperName() != null) sb.append(" extends ").append(cw.getOriginalSuperName());
        if (cw.getOriginalInterfaces() != null)
            for (String s : cw.getOriginalInterfaces()) sb.append(" implements ").append(s);
        filter.accept(sb.toString());
        sb.append(" * *");
        filter.accept(sb.toString());
        sb.append("(*)");
        filter.accept(sb.toString());
    }

    protected final Map<String, ClassWrapper> getClasses() {
        return this.obfuscator.classes;
    }

    protected final Collection<ClassWrapper> getClassWrappers() {
        return this.obfuscator.classes.values();
    }

    protected final Map<String, ClassWrapper> getClassPath() {
        return this.obfuscator.classpath;
    }

    protected final Map<String, byte[]> getResources() {
        return this.obfuscator.resources;
    }

    protected String randomClassName() {
        Collection<String> classNames = getClasses().keySet();
        ArrayList<String> list = new ArrayList<>(classNames);

        String first = list.get(RandomUtils.getRandomInt(classNames.size()));
        String second = list.get(RandomUtils.getRandomInt(classNames.size()));

        return first + '$' + second.substring(second.lastIndexOf("/") + 1);
    }


    protected final boolean hasAnnotation(ClassWrapper classWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(classWrapper, annotation());
    }

    protected final boolean hasAnnotation(MethodWrapper methodWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(methodWrapper, annotation());
    }

    protected final boolean hasAnnotation(FieldWrapper fieldWrapper) {
        if (annotation() == null)
            return false;
        return ASMUtils.hasAnnotation(fieldWrapper, annotation());
    }

    protected final Map<String, String> getAnnotationValues(ClassWrapper classWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(classWrapper, annotation());
    }

    protected final Map<String, String> getAnnotationValues(MethodWrapper methodWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(methodWrapper, annotation());
    }

    protected final Map<String, String> getAnnotationValues(FieldWrapper fieldWrapper) {
        if (annotation() == null)
            return null;
        return ASMUtils.getAnnotationValues(fieldWrapper, annotation());
    }

    protected final boolean matchAnnotation(ClassWrapper classWrapper) {
        Map<String, String> map = getAnnotationValues(classWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true"));
    }

    protected final boolean matchAnnotation(MethodWrapper methodWrapper) {
        Map<String, String> map = getAnnotationValues(methodWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true"));
    }

    protected final boolean matchAnnotation(FieldWrapper fieldWrapper) {
        Map<String, String> map = getAnnotationValues(fieldWrapper);
        return Boolean.parseBoolean(Objects.requireNonNull(map).getOrDefault("obfuscated", "true"));
    }
}
