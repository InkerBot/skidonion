package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals.*;

import java.util.List;

public class InternalClasses {
    public static void inject(Transformer transformer, List<ClassWrapper> classes) throws Exception {
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(HttpUtilsDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(MachineIDUtilsDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(QQUtilsDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(VerifyUtilsDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(URLEncoderDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(EdDSAEngineDump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(ChaCha20Dump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
        {
            ClassNode node = new ClassNode();
            ClassReader reader = new ClassReader(Base64Dump.dump());
            reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            classes.add(transformer.injectClass(node));
        }
    }
}
