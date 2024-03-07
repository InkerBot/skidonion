package tech.skidonion.obfuscator.transformer.impl;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.CustomClassWriter;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
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
import tech.skidonion.obfuscator.utils.ASMUtils;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.StringUtils;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ClassPackageValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;

public class NativeObfuscation extends Transformer {

    private final BooleanValue print_instructions = new BooleanValue("print_instructions", false);
    private final ClassPackageValue loader_package = new ClassPackageValue("loader_package", "skidonion/??????");
    private final BooleanValue hidden_stack_trace = new BooleanValue("hidden_stack_trace", true);

    public NativeObfuscation(String name) {
        super(name, false);
        addSettings(print_instructions, loader_package, hidden_stack_trace);
    }

    private Snippets snippets;
    private StringPool stringPool;
    private MethodProcessor methodProcessor;
    private NodeCache<String> cachedStrings;
    private NodeCache<String> cachedClasses;
    private NodeCache<CachedMethodInfo> cachedMethods;
    private NodeCache<CachedFieldInfo> cachedFields;
    private AtomicInteger cachedCallSitesIndex;
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
        nativeDir = loader_package.getValue();
        nativeDir = nativeDir.substring(0, nativeDir.length() - 1);
    }


    @Override
    public void transform() throws Exception {
        Path cppDir = print_instructions.isEnable() ? new File(obfuscator.getConfig().getString("output")).getParentFile().toPath() : Files.createTempDirectory(null);
        Path cppOutput = cppDir.resolve("output");
        Files.createDirectories(cppOutput);
        CppCompiler compiler = obfuscator.getCompiler();
        compiler.setOutputDir(cppDir.toFile());

        FileUtils.copyResource("sources/jni.h", cppDir);
        FileUtils.copyResource("sources/jni_md.h", cppDir);
        FileUtils.copyResource("sources/native_jvm.cpp", cppDir);
        FileUtils.copyResource("sources/native_jvm.hpp", cppDir);
        FileUtils.copyResource("sources/native_jvm_output.hpp", cppDir);
        FileUtils.copyResource("sources/string_pool.hpp", cppDir);
        compiler.addCppFile(cppDir.resolve("native_jvm.cpp").toAbsolutePath().toString());
        compiler.addCppFile(cppDir.resolve("string_pool.cpp").toAbsolutePath().toString());
        compiler.addCppFile(cppDir.resolve("native_jvm_output.cpp").toAbsolutePath().toString());

//        CMakeFilesBuilder cMakeBuilder = new CMakeFilesBuilder(projectName);
//        cMakeBuilder.addMainFile("native_jvm.hpp");
//        cMakeBuilder.addMainFile("native_jvm.cpp");
//        cMakeBuilder.addMainFile("native_jvm_output.hpp");
//        cMakeBuilder.addMainFile("native_jvm_output.cpp");
//        cMakeBuilder.addMainFile("string_pool.hpp");
//        cMakeBuilder.addMainFile("string_pool.cpp");

        MainSourceBuilder mainSourceBuilder = new MainSourceBuilder();

        hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/___");

        Integer[] classIndexReference = new Integer[]{0};

        getFilteredClasses().forEach(cw -> {
            String clinitVirtualization = "NONE";
            {
                Map<String, Object> map = getAnnotationValues(cw);
                removeAnnotation(cw);
                if (map != null) {
                    Object virtualize = map.get("virtualize");
                    if (virtualize instanceof String[]) {
                        clinitVirtualization = ((String[]) virtualize)[1];
                        compiler.getVirtualizeMacroCount().getAndIncrement();
                    }
                }
            }
            try {
                StringBuilder nativeMethods = new StringBuilder();
                List<HiddenCppMethod> hiddenMethods = new ArrayList<>();

                PhantomShield.INFO("Converting to JNI: {}", cw.getOriginalName());

                cw.getMethods().stream().filter(this::match)
                        .map(MethodWrapper::getMethodNode)
                        .filter(MethodProcessor::shouldProcess)
                        .forEach(methodNode -> PreprocessorRunner.preprocess(this, cw.getClassNode(), methodNode));

                CustomClassWriter computedWriter = new CustomClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, obfuscator);
                cw.getClassNode().accept(computedWriter);

                ClassReader computedReader = new ClassReader(computedWriter.toByteArray());
                ClassNode computedClassNode = new ClassNode(Opcodes.ASM9);
                computedReader.accept(computedClassNode, 0);

                IntStream.range(0, computedClassNode.methods.size())
                        .forEach(i -> cw.getMethods().get(i).setMethodNode(computedClassNode.methods.get(i)));
                IntStream.range(0, computedClassNode.fields.size())
                        .forEach(i -> cw.getFields().get(i).setFieldNode(computedClassNode.fields.get(i)));

                cw.setClassNode(computedClassNode);
                cw.getOrCreateClinit();

                cachedStrings.clear();
                cachedClasses.clear();
                cachedMethods.clear();
                cachedFields.clear();
                cachedCallSitesIndex = new AtomicInteger();

                try (ClassSourceBuilder cppBuilder =
                             new ClassSourceBuilder(cppOutput, cw.getName(), classIndexReference[0]++, stringPool)) {
                    compiler.addCppFile(cppBuilder.getCppFile().toAbsolutePath().toString());
                    StringBuilder instructions = new StringBuilder();

                    boolean shouldVirtualize = false;
                    for (int i = 0; i < cw.getMethods().size(); i++) {
                        MethodWrapper method = cw.getMethods().get(i);

                        if (!MethodProcessor.shouldProcess(method.getMethodNode()) || !match(method)) {
                            continue;
                        }
                        MethodContext context = new MethodContext(this, method, i, cw, currentClassId);
                        Map<String, Object> map = getAnnotationValues(method);
                        removeAnnotation(method);
                        if (map != null) {
                            Object virtualize = map.get("virtualize");
                            if (virtualize instanceof String[]) {
                                shouldVirtualize = true;
                                context.virtualization = ((String[]) virtualize)[1];
                                compiler.getVirtualizeMacroCount().getAndIncrement();
                            }
                        }
                        if ("<clinit>".equals(method.getName())) {
                            context.virtualization = clinitVirtualization;
                        }
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


                    cppBuilder.addHeader(cachedStrings.size(), cachedClasses.size(), cachedMethods.size(), cachedFields.size(), cachedCallSitesIndex.get(), shouldVirtualize);
                    cppBuilder.addInstructions(instructions.toString());
                    cppBuilder.registerMethods(cachedStrings, cachedClasses, nativeMethods.toString(), hiddenMethods);

//                    cMakeBuilder.addClassFile("output/" + cppBuilder.getHppFilename());
//                    cMakeBuilder.addClassFile("output/" + cppBuilder.getCppFilename());

                    mainSourceBuilder.addHeader(cppBuilder.getHppFilename());
                    mainSourceBuilder.registerClassMethods(currentClassId, cppBuilder.getFilename());
                }

                currentClassId++;
            } catch (IOException ex) {
                ERROR("Error while processing {}", cw.getOriginalName(), ex);
            }

        });

        if (hidden_stack_trace.isEnable()) {
            for (ClassNode hiddenClass : hiddenMethodsPool.getClasses()) {

                String hiddenClassFileName = "data_" + StringUtils.escapeCppNameString(hiddenClass.name.replace('/', '_'));

//            cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".hpp");
//            cMakeBuilder.addClassFile("output/" + hiddenClassFileName + ".cpp");

                mainSourceBuilder.addHeader(hiddenClassFileName + ".hpp");
                mainSourceBuilder.registerDefine(stringPool.get(hiddenClass.name), hiddenClassFileName);

                CustomClassWriter classWriter = new CustomClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, obfuscator);
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

                Path cppPath = cppOutput.resolve(hiddenClassFileName + ".cpp");
                try (BufferedWriter cppWriter = Files.newBufferedWriter(cppPath)) {
                    compiler.addCppFile(cppPath.toAbsolutePath().toString());
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
        } else {
            injectClassesAsResource(hiddenMethodsPool.getClasses());
        }

        Files.write(cppDir.resolve("string_pool.cpp"), stringPool.build().getBytes(StandardCharsets.UTF_8));

        Files.write(cppDir.resolve("native_jvm_output.cpp"), mainSourceBuilder.build(nativeDir, currentClassId)
                .getBytes(StandardCharsets.UTF_8));

        if (compiler.getVirtualizeMacroCount().get() > 0) {
            FileUtils.copyResource("sources/VirtualizerSDK.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_BorlandC_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs_BorlandC_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs_GNU_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs_ICL_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs_LCC_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_CustomVMs_VC_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_GNU_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_ICL_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_LCC_inline.h", cppDir);
            FileUtils.copyResource("sources/VirtualizerSDK_VC_inline.h", cppDir);
        }

        compiler.compile(StringUtils.createStringMap("loader_path", nativeDir));

        if (!print_instructions.isEnable()) {
            FileUtils.clearDirectory(cppDir);
        }
    }

    @Override
    public void preprocess() throws Exception {
        this.init();

        String loaderClassName = nativeDir + "/___";

        ClassNode loaderClass;

        List<ClassNode> classNodes = ASMUtils.readClassesWithInputStream("/binaries/phantomshield-loader.bin");
        if (classNodes.size() != 1) throw new RuntimeException("impossible loader class member size");

        loaderClass = classNodes.get(0);
        loaderClass.sourceFile = "synthetic";

        ClassNode resultLoaderClass = new ClassNode(Opcodes.ASM9);
        String originalLoaderClassName = loaderClass.name;
        loaderClass.accept(new ClassRemapper(resultLoaderClass, new Remapper() {
            @Override
            public String map(String internalName) {
                return internalName.equals(originalLoaderClassName) ? loaderClassName : internalName;
            }
        }));
        injectClassesAsResource(Collections.singletonList(resultLoaderClass));
    }

    @Override
    public String annotation() {
        return Type.getDescriptor(tech.skidonion.obfuscator.annotations.NativeObfuscation.class);
    }

    public AtomicInteger getCachedCallSitesIndex() {
        return cachedCallSitesIndex;
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
