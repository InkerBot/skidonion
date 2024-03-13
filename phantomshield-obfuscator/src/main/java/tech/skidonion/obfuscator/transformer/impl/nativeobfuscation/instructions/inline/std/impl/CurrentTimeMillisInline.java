package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.impl;

import org.objectweb.asm.tree.MethodInsnNode;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.instructions.inline.std.AbstractStandardMethodInline;

import java.util.Objects;

public class CurrentTimeMillisInline extends AbstractStandardMethodInline {
    @Override
    public void process(String desc, MethodContext context, MethodInsnNode node) {
        if (Objects.equals(desc, "java/lang/System.currentTimeMillis()J")) {
            context.headers.add("<chrono>");
            context.output.append("cstack").append(context.stackPointer).append(".j = (jlong) std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count();\n");
        }
    }

    @Override
    public String[] methods() {
        return new String[]{"java/lang/System.currentTimeMillis()J"};
    }
}
