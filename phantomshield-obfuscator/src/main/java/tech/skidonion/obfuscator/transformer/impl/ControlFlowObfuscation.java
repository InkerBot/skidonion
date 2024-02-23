package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.generic.ResolvedBlocks;
import tech.skidonion.obfuscator.transformer.generic.resolver.CodeBlockResolver;

public class ControlFlowObfuscation extends Transformer {
    public ControlFlowObfuscation(String name) {
        super(name);
    }

    @Override
    public void transform() throws Exception {
        getFilteredClasses().forEach(cw -> cw.getMethods().stream().filter(this::match).forEach(wrapper -> {
            MethodNode method = wrapper.getMethodNode();

            ResolvedBlocks resolve = CodeBlockResolver.resolve(method);
            // shuffler labels orders


            method.instructions = resolve.toInsnList();

//            Frame<SourceValue>[] frames;
//            try {
//                frames = new Analyzer<>(new SourceInterpreter()).analyze(node.name, node);
//            } catch (AnalyzerException e) {
//                throw new RuntimeException(e);
//            }


        }));
    }

    @Override
    public void preprocess() throws Exception {
    }

    @Override
    public String annotation() {
        return null;
    }

}
