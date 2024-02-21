package tech.skidonion.obfuscator.trace;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class JarUtils {

    public static List<ClassNode> readJar(File file) {
        List<ClassNode> classes = new ArrayList<>();
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file.toPath()))) {
            for (ZipEntry entry; (entry = zis.getNextEntry()) != null; ) {
                if (!entry.getName().endsWith(".class")) continue;
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] n = new byte[4096];
                for (int length; (length = zis.read(n, 0, 4096)) != -1; ) bytes.write(n, 0, length);
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(bytes.toByteArray());
                reader.accept(node, 0);
                classes.add(node);
            }
            return classes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
