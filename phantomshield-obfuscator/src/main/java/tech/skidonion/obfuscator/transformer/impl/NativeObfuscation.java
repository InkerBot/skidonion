package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.CustomClassWriter;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenCppMethod;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenMethodsPool;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode.PreprocessorRunner;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedFieldInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedMethodInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.NodeCache;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.Snippets;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.ClassSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.MainSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.StringPool;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ModeValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NativeObfuscation extends Transformer {

    private final BooleanValue print_instructions = new BooleanValue("print_instructions", false);
    private final ModeValue invokedynamic_mode = new ModeValue("invokedynamic_mode", "compatibility", "compatibility", "enhancement");

    public NativeObfuscation(String name) {
        super(name, false);
        addSettings(print_instructions, invokedynamic_mode);
    }

    private Snippets snippets;
    private StringPool stringPool;
    private MethodProcessor methodProcessor;

    private NodeCache<String> cachedStrings;
    private NodeCache<String> cachedClasses;
    private NodeCache<CachedMethodInfo> cachedMethods;
    private NodeCache<CachedFieldInfo> cachedFields;
    private HiddenMethodsPool hiddenMethodsPool;
    private int currentClassId;
    private String nativeDir;

    private void init() {
        stringPool = new StringPool();
        snippets = new Snippets(stringPool);
        cachedStrings = new NodeCache<>("(cstrings[%d])");
        cachedClasses = new NodeCache<>("(cclasses[%d])");
        cachedMethods = new NodeCache<>("(cmethods[%d])");
        cachedFields = new NodeCache<>("(cfields[%d])");
        methodProcessor = new MethodProcessor(this);
    }


    @Override
    public void transform() throws Exception {
        this.init();

        Path cppDir = print_instructions.isEnable() ? new File(obfuscator.getConfig().getAsJsonPrimitive("output").getAsString()).getParentFile().toPath() : Files.createTempDirectory(null);
        Path cppOutput = cppDir.resolve("output");
        Files.createDirectories(cppOutput);

        FileUtils.copyResource("sources/native_jvm.cpp", cppDir);
        FileUtils.copyResource("sources/native_jvm.hpp", cppDir);
        FileUtils.copyResource("sources/native_jvm_output.hpp", cppDir);
        FileUtils.copyResource("sources/string_pool.hpp", cppDir);

//        CMakeFilesBuilder cMakeBuilder = new CMakeFilesBuilder(projectName);
//        cMakeBuilder.addMainFile("native_jvm.hpp");
//        cMakeBuilder.addMainFile("native_jvm.cpp");
//        cMakeBuilder.addMainFile("native_jvm_output.hpp");
//        cMakeBuilder.addMainFile("native_jvm_output.cpp");
//        cMakeBuilder.addMainFile("string_pool.hpp");
//        cMakeBuilder.addMainFile("string_pool.cpp");

        MainSourceBuilder mainSourceBuilder = new MainSourceBuilder();


        nativeDir = "skidonion/" + RandomUtils.getRandomLetters(8);
        hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/hidden");

        Integer[] classIndexReference = new Integer[]{0};

        getFilteredClasses().forEach(cw -> {
            try {
                StringBuilder nativeMethods = new StringBuilder();
                List<HiddenCppMethod> hiddenMethods = new ArrayList<>();

                PhantomShield.INFO("Converting to JNI: {}", cw.getOriginalName());

                cw.getMethods().stream().filter(this::match)
                        .map(MethodWrapper::getMethodNode)
                        .filter(MethodProcessor::shouldProcess)
                        .forEach(methodNode -> PreprocessorRunner.preprocess(cw.getClassNode(), methodNode, invokedynamic_mode));

                ClassWriter computedWriter = new CustomClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, obfuscator);
                cw.getClassNode().accept(computedWriter);

                ClassReader computedReader = new ClassReader(computedWriter.toByteArray());
                ClassNode computedClassNode = new ClassNode(Opcodes.ASM9);
                computedReader.accept(computedClassNode, 0);
                cw.setClassNode(computedClassNode);


                if (computedClassNode.methods.stream().noneMatch(x -> x.name.equals("<clinit>"))) {
                    computedClassNode.methods.add(new MethodNode(Opcodes.ASM9, Opcodes.ACC_STATIC,
                            "<clinit>", "()V", null, new String[0]));
                }

                cachedStrings.clear();
                cachedClasses.clear();
                cachedMethods.clear();
                cachedFields.clear();

                try (ClassSourceBuilder cppBuilder =
                             new ClassSourceBuilder(cppOutput, cw.getName(), classIndexReference[0]++, stringPool)) {
                    StringBuilder instructions = new StringBuilder();


                    for (int i = 0; i < cw.getMethods().size(); i++) {
                        MethodWrapper method = cw.getMethods().get(i);

                        if (!MethodProcessor.shouldProcess(method.getMethodNode()) || !match(method)) {
                            continue;
                        }

                        MethodContext context = new MethodContext(this, method, i, cw, currentClassId);
                        methodProcessor.processMethod(context);
                        instructions.append(context.output.toString().replace("\n", "\n    "));

                        nativeMethods.append(context.nativeMethods);

                        if (context.proxyMethod != null) {
                            hiddenMethods.add(new HiddenCppMethod(context.proxyMethod, context.cppNativeMethodName));
                        }

                        if ((computedClassNode.access & Opcodes.ACC_INTERFACE) > 0) {
                            method.getMethodNode().access &= ~Opcodes.ACC_NATIVE;
                        }
                    }


                    cppBuilder.addHeader(cachedStrings.size(), cachedClasses.size(), cachedMethods.size(), cachedFields.size());
                    cppBuilder.addInstructions(instructions.toString());
                    cppBuilder.registerMethods(cachedStrings, cachedClasses, nativeMethods.toString(), hiddenMethods);

//                    cMakeBuilder.addClassFile("output/" + cppBuilder.getHppFilename());
//                    cMakeBuilder.addClassFile("output/" + cppBuilder.getCppFilename());

                    mainSourceBuilder.addHeader(cppBuilder.getHppFilename());
                    mainSourceBuilder.registerClassMethods(currentClassId, cppBuilder.getFilename());
                }

                currentClassId++;
            } catch (IOException ex) {
                PhantomShield.LOGGER.error("Error while processing {}", cw.getOriginalName(), ex);
            }

        });

        for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {
            String hiddenClassFileName = "data_" + StringUtils.escapeCppNameString(hiddenClass.name.replace('/', '_'));

//            cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".hpp");
//            cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".cpp");

            mainSourceBuilder.addHeader(hiddenClassFileName + ".hpp");
            mainSourceBuilder.registerDefine(stringPool.get(hiddenClass.name), hiddenClassFileName);

            ClassWriter classWriter = new CustomClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, obfuscator);
            hiddenClass.accept(classWriter);
            byte[] rawData = classWriter.toByteArray();
            List<Byte> data = new ArrayList<>(rawData.length);
            for (byte b : rawData) {
                data.add(b);
            }

            try (BufferedWriter hppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".hpp"))) {
                hppWriter.append("#include \"../native_jvm.hpp\"\n\n");
                hppWriter.append("#ifndef ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                hppWriter.append("#define ").append(hiddenClassFileName.toUpperCase()).append("_HPP_GUARD\n\n");
                hppWriter.append("namespace native_jvm::data::__ngen_").append(hiddenClassFileName).append(" {\n");
                hppWriter.append("    const jbyte* get_class_data();\n");
                hppWriter.append("    const jsize get_class_data_length();\n");
                hppWriter.append("}\n\n");
                hppWriter.append("#endif\n");
            }

            try (BufferedWriter cppWriter = Files.newBufferedWriter(cppOutput.resolve(hiddenClassFileName + ".cpp"))) {
                cppWriter.append("#include \"").append(hiddenClassFileName).append(".hpp\"\n\n");
                cppWriter.append("namespace native_jvm::data::__ngen_").append(hiddenClassFileName).append(" {\n");
                cppWriter.append("    static const jbyte class_data[").append(String.valueOf(data.size())).append("] = { ");
                cppWriter.append(data.stream().map(String::valueOf).collect(Collectors.joining(", ")));
                cppWriter.append("};\n");
                cppWriter.append("    static const jsize class_data_length = ").append(String.valueOf(data.size())).append(";\n\n");
                cppWriter.append("    const jbyte* get_class_data() { return class_data; }\n");
                cppWriter.append("    const jsize get_class_data_length() { return class_data_length; }\n");
                cppWriter.append("}\n");
            }
        }

        // TODO: inject loader

//        String loaderClassName = nativeDir + "/Loader";
//
//        ClassNode loaderClass;
//
//        if (plainLibName == null) {
//            ClassReader loaderClassReader = new ClassReader(Objects.requireNonNull(NativeObfuscator.class
//                    .getResourceAsStream("compiletime/LoaderUnpack.class")));
//            loaderClass = new ClassNode(Opcodes.ASM9);
//            loaderClassReader.accept(loaderClass, 0);
//            loaderClass.sourceFile = "synthetic";
//            System.out.println("/" + nativeDir + "/");
//        } else {
//            ClassReader loaderClassReader = new ClassReader(Objects.requireNonNull(NativeObfuscator.class
//                    .getResourceAsStream("compiletime/LoaderPlain.class")));
//            loaderClass = new ClassNode(Opcodes.ASM9);
//            loaderClassReader.accept(loaderClass, 0);
//            loaderClass.sourceFile = "synthetic";
//            loaderClass.methods.forEach(method -> {
//                for (int i = 0; i < method.instructions.size(); i++) {
//                    AbstractInsnNode insnNode = method.instructions.get(i);
//                    if (insnNode instanceof LdcInsnNode && ((LdcInsnNode) insnNode).cst instanceof String &&
//                            ((LdcInsnNode) insnNode).cst.equals("%LIB_NAME%")) {
//                        ((LdcInsnNode) insnNode).cst = plainLibName;
//                    }
//                }
//            });
//        }
//
//        ClassNode resultLoaderClass = new ClassNode(Opcodes.ASM9);
//        String originalLoaderClassName = loaderClass.name;
//        loaderClass.accept(new ClassRemapper(resultLoaderClass, new Remapper() {
//            @Override
//            public String map(String internalName) {
//                return internalName.equals(originalLoaderClassName) ? loaderClassName : internalName;
//            }
//        }));
//
//        ClassWriter classWriter = new SafeClassWriter(metadataReader, Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
//        resultLoaderClass.accept(classWriter);
//        Util.writeEntry(out, loaderClassName + ".class", classWriter.toByteArray());

        Files.write(cppDir.resolve("string_pool.cpp"), stringPool.build().getBytes(StandardCharsets.UTF_8));

        Files.write(cppDir.resolve("native_jvm_output.cpp"), mainSourceBuilder.build(nativeDir, currentClassId)
                .getBytes(StandardCharsets.UTF_8));

    }

    @Override
    public void preprocess() throws Exception {

    }

    public Snippets getSnippets() {
        return snippets;
    }

    public StringPool getStringPool() {
        return stringPool;
    }

    public NodeCache<String> getCachedStrings() {
        return cachedStrings;
    }

    public NodeCache<String> getCachedClasses() {
        return cachedClasses;
    }

    public NodeCache<CachedMethodInfo> getCachedMethods() {
        return cachedMethods;
    }

    public NodeCache<CachedFieldInfo> getCachedFields() {
        return cachedFields;
    }

    public String getNativeDir() {
        return nativeDir;
    }

    public HiddenMethodsPool getHiddenMethodsPool() {
        return hiddenMethodsPool;
    }

}
