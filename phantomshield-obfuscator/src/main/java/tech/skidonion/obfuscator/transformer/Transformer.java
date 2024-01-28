package tech.skidonion.obfuscator.transformer;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.filter.Filter;
import tech.skidonion.obfuscator.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public abstract class Transformer implements Opcodes {
    protected PhantomShield obfuscator;
    private final boolean forceEnabled;
    private final String name;
    private boolean enabled;
    private Filter filter;
    private List<Value<?>> settings = new ArrayList<>();

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

    protected void injectClass(Collection<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            ClassWrapper cw = new ClassWrapper(classNode, false);
            obfuscator.classes.put(cw.getName(), cw);
        }
    }

    protected void injectClassAsResource(Collection<ClassNode> classNodes) {
        for (ClassNode classNode : classNodes) {
            ClassWriter cw = new ClassWriter(0);
            classNode.accept(cw);
            obfuscator.resources.put(classNode.name + ".class", cw.toByteArray());
        }
    }

    protected void addSetting(Value<?> setting) {
        settings.add(setting);
    }

    protected void addSettings(Value<?>... settings) {
        for (Value<?> setting : settings) {
            addSetting(setting);
        }
    }

    public Value[] getSettings() {
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
        if (filter == null) return true;
        return filter.match(method);
    }

    protected boolean match(FieldWrapper field) {
        if (filter == null) return true;
        return filter.match(field);
    }

    protected boolean match(ClassWrapper clazz) {
        if (filter == null) return true;
        return filter.match(clazz);
    }

    protected final Stream<ClassWrapper> getFilteredClasses() {
        return getClassWrappers().stream().filter(this::match);
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

}
