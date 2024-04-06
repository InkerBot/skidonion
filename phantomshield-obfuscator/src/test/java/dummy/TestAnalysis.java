package dummy;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.*;
import tech.skidonion.obfuscator.asm.SimpleInterpreter;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestAnalysis {
    public static void main(String[] args) throws IOException, AnalyzerException {
        ClassReader reader = new ClassReader(Files.readAllBytes(Paths.get("GaussUtils.class")));
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        for (MethodNode method : node.methods) {
            Frame<BasicValue>[] frames = new Analyzer<>(new SimpleInterpreter()).analyze(node.name,method);

        }
    }
}
