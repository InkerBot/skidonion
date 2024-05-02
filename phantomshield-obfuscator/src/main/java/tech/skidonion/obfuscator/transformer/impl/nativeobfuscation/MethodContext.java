package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.transformer.impl.NativeObfuscation;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedFieldInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedMethodInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.NodeCache;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.Snippets;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.StringPool;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.verification.BufferContext;

import java.util.*;

public class MethodContext {

    public NativeObfuscation obfuscator;
    public final MethodWrapper method;
    public final ClassWrapper clazz;
    public final int methodIndex;
    public final int classIndex;

    public final StringBuilder output;
    public final StringBuilder export;
    public final StringBuilder nativeMethods;

    public Type ret;
    public ArrayList<Type> argTypes;

    public int line;
    public List<Integer> stack;
    public List<Integer> locals;
    public Set<TryCatchBlockNode> tryCatches;
    public Map<CatchesBlock, String> catches;

    public HiddenMethodsPool.HiddenMethod proxyMethod;
    public MethodNode nativeMethod;
    public int stackPointer;
    private final LabelPool labelPool = new LabelPool();
    public String cppNativeMethodName;
    public String virtualization = "NONE";
    public boolean manualTryCatch = false;
    public final String prefixVM;
    public boolean shouldVirtualize = false;
    public Set<String> headers = new HashSet<>();
    public BufferContext verificationLock;


    public MethodContext(NativeObfuscation obfuscator, MethodWrapper method, int methodIndex, ClassWrapper clazz,
                         int classIndex) {
        this.obfuscator = obfuscator;
        this.method = method;
        this.methodIndex = methodIndex;
        this.clazz = clazz;
        this.classIndex = classIndex;
        this.prefixVM = obfuscator.obfuscator.getCompiler().isAdvancedModuleEnable() ? "VM" : "VIRTUALIZER";

        this.output = new StringBuilder();
        this.export = new StringBuilder();
        this.nativeMethods = new StringBuilder();

        this.line = -1;
        this.stack = new ArrayList<>();
        this.locals = new ArrayList<>();
        this.tryCatches = new HashSet<>();
        this.catches = new HashMap<>();
    }


    public NodeCache<String> getCachedStrings() {
        return obfuscator.getCachedStrings();
    }

    public NodeCache<String> getCachedClasses() {
        return obfuscator.getCachedClasses();
    }

    public NodeCache<CachedMethodInfo> getCachedMethods() {
        return obfuscator.getCachedMethods();
    }

    public NodeCache<CachedFieldInfo> getCachedFields() {
        return obfuscator.getCachedFields();
    }

    public Snippets getSnippets() {
        return obfuscator.getSnippets();
    }

    public StringPool getStringPool() {
        return obfuscator.getStringPool();
    }

    public LabelPool getLabelPool() {
        return labelPool;
    }

    public void injectHeader() {
        if (!"NONE".equals(virtualization)) {
            output.append(prefixVM).append("_").append(virtualization).append("_START\n");
            output.append("volatile bool __dummy = true;\n");
            output.append("if(__dummy){\n");
        }
    }

    public void injectTail() {
        if (!"NONE".equals(virtualization)) {
            output.append("}\n");
            output.append(prefixVM).append("_").append(virtualization).append("_END\n");
        }
    }
}
