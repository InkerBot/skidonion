package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std;

import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.impl.SystemInline;

import java.util.*;

public class InlineRegister {
    private final List<AbstractStandardMethodInline> inlines = new ArrayList<>();

    public InlineRegister() {
        inlines.add(new SystemInline());
    }

    public void process(String desc, MethodContext context, MethodInsnNode node) {
        for (AbstractStandardMethodInline inline : inlines) {
            inline.process(desc, context, node);

        }
    }

    public Set<String> init() {
        Set<String> methods = new HashSet<>();
        for (AbstractStandardMethodInline inline : inlines) {
            methods.addAll(Arrays.asList(inline.methods()));
        }
        return methods;
    }
}
