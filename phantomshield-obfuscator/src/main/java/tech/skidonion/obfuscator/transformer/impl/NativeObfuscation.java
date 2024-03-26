package tech.skidonion.obfuscator.transformer.impl;

import com.google.gson.JsonObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.CustomClassWriter;
import tech.skidonion.obfuscator.asm.MethodWrapper;
import tech.skidonion.obfuscator.cpp.CppCompiler;
import tech.skidonion.obfuscator.inline.Wrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenCppMethod;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.HiddenMethodsPool;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodContext;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.MethodProcessor;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.bytecode.PreprocessorRunner;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedFieldInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.CachedMethodInfo;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.caches.NodeCache;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals.HttpUtils$OnHttpResultDump;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals.HttpUtilsDump;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals.QQUtilsDump;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.Snippets;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.ClassSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.InlineSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.MainSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.StringPool;
import tech.skidonion.obfuscator.transformer.impl.renamer.Mapper;
import tech.skidonion.obfuscator.utils.*;
import tech.skidonion.obfuscator.value.impls.BooleanValue;
import tech.skidonion.obfuscator.value.impls.ClassPackageValue;
import tech.skidonion.obfuscator.value.impls.StringValue;
import tech.skidonion.obfuscator.value.impls.SubValue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class NativeObfuscation extends Transformer {
    public final Map<String, MethodWrapper> injectedWrapperMethods = new HashMap<>();
    private final BooleanValue print_instructions = new BooleanValue("print_instructions", false);
    private final ClassPackageValue loader_package = new ClassPackageValue("loader_package", "skidonion/??????");
    private final BooleanValue hidden_stack_trace = new BooleanValue("hidden_stack_trace", true);

    private final BooleanValue verification_enable = new BooleanValue("verification_enable", false);
    private final BooleanValue use_internal_user_interface = new BooleanValue("user_internal_user_interface", true);
    private final StringValue verification_server = new StringValue("verification_server", "https://skidonion.tech/");
    private final StringValue verification_software_id = new StringValue("verification_software_id", "-1");
    private final StringValue verification_token = new StringValue("verification_token", "");
    private final SubValue verification = new SubValue("verification", verification_enable, use_internal_user_interface, verification_server, verification_software_id, verification_token);

    public NativeObfuscation(String name) {
        super(name, false);
        addSettings(print_instructions, loader_package, hidden_stack_trace, verification);
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

        InlineSourceBuilder inlineSourceBuilder = new InlineSourceBuilder(this, compiler);

        hiddenMethodsPool = new HiddenMethodsPool(nativeDir + "/___");

        Integer[] classIndexReference = new Integer[]{0};
        AtomicInteger internalIndex = new AtomicInteger();
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
                String displayName = cw.getOriginalName();
                boolean isInternal = displayName.startsWith("tech/skidonion/verification/");
                if (isInternal) displayName = "[Internal Class" + internalIndex.getAndIncrement() + "]";

                INFO("Converting to JNI: {}", displayName);

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
                             new ClassSourceBuilder(this, cppOutput, cw.getName(), classIndexReference[0]++, stringPool)) {
                    compiler.addCppFile(cppBuilder.getCppFile().toAbsolutePath().toString());
                    StringBuilder instructions = new StringBuilder();

                    Set<String> headers = new HashSet<>();

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
                            if (!"NONE".equals(clinitVirtualization)) {
                                shouldVirtualize = true;
                                context.virtualization = clinitVirtualization;
                            }
                        }
                        methodProcessor.processMethod(context);
                        shouldVirtualize |= context.shouldVirtualize;

                        headers.addAll(context.headers);

                        instructions.append(context.output.toString().replace("\n", "\n    "));

                        nativeMethods.append(context.nativeMethods);

                        if (context.proxyMethod != null) {
                            hiddenMethods.add(new HiddenCppMethod(context.proxyMethod, context.cppNativeMethodName));
                        }

                        if ((computedClassNode.access & Opcodes.ACC_INTERFACE) > 0) {
                            method.getMethodNode().access &= ~Opcodes.ACC_NATIVE;
                        }
                    }

                    shouldVirtualize |= isVerificationEnable();

                    cppBuilder.addHeader(headers, cachedStrings.size(), cachedClasses.size(), cachedMethods.size(), cachedFields.size(), cachedCallSitesIndex.get(), shouldVirtualize);
                    cppBuilder.addInstructions(instructions.toString());
                    cppBuilder.registerMethods(cachedStrings, cachedClasses, nativeMethods.toString(), hiddenMethods, shouldVirtualize, isInternal);

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

        Files.write(cppDir.resolve("native_jvm_output.cpp"), mainSourceBuilder.build(nativeDir, currentClassId).getBytes(StandardCharsets.UTF_8));

        Files.write(cppDir.resolve("native_jvm_inline.cpp"), inlineSourceBuilder.buildCpp().getBytes(StandardCharsets.UTF_8));
        Files.write(cppDir.resolve("native_jvm_inline.hpp"), inlineSourceBuilder.buildHpp().getBytes(StandardCharsets.UTF_8));

        compiler.addCppFile(cppDir.resolve("native_jvm_inline.cpp").toAbsolutePath().toString());

        if (compiler.getVirtualizeMacroCount().get() > 0) {
            if (compiler.isAdvancedModuleEnable()) {
                FileUtils.copyResource("sources/ThemidaSDK.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs_BorlandC_inline.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs_GNU_inline.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs_ICL_inline.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs_LCC_inline.h", cppDir);
                FileUtils.copyResource("sources/SecureEngineCustomVMs_VC_inline.h", cppDir);
            } else {
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

        ClassNode resultLoaderClass = new ClassNode();
        String originalLoaderClassName = loaderClass.name;
        loaderClass.accept(new ClassRemapper(resultLoaderClass, new Remapper() {
            @Override
            public String map(String internalName) {
                return internalName.equals(originalLoaderClassName) ? loaderClassName : internalName;
            }
        }));
        injectClassesAsResource(Collections.singletonList(resultLoaderClass));

        List<ClassWrapper> injected = new LinkedList<>();
        long verifySoftwareId;
        String verifyPublicKey;
        try {
            if (isVerificationEnable()) {
                JsonObject softwareInformation = VerifyUtils.requestSoftwareInformation(this.verification_server.getValue(), String.valueOf(Wrapper.getUserId()), this.verification_token.getValue(), this.verification_software_id.getValue());
                if (softwareInformation == null || softwareInformation.getAsJsonPrimitive("code").getAsLong() != 0L) {
                    ERROR("Can't request software information");
                    return;
                }
                verifySoftwareId = softwareInformation.getAsJsonPrimitive("id").getAsLong();
                verifyPublicKey = softwareInformation.getAsJsonPrimitive("public_key").getAsString();
                INFO("Software Name: {}", softwareInformation.getAsJsonPrimitive("software_name").getAsString());
                List<ClassWrapper> classes = injectClasses(ASMUtils.readClassesWithInputStream("/binaries/phantomshield-verification.bin"));
                {
                    ClassNode node = new ClassNode();
                    ClassReader reader = new ClassReader(HttpUtilsDump.dump());
                    reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    classes.add(injectClass(node));
                }
                {
                    ClassNode node = new ClassNode();
                    ClassReader reader = new ClassReader(HttpUtils$OnHttpResultDump.dump());
                    reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    classes.add(injectClass(node));
                }
                {
                    ClassNode node = new ClassNode();
                    ClassReader reader = new ClassReader(QQUtilsDump.dump());
                    reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                    classes.add(injectClass(node));
                }
                for (ClassWrapper cw : classes) {
                    String origin = cw.getOriginalName();
                    addInternalInclusion(origin, "*");
                    for (MethodWrapper mw : cw.getMethods()) {
                        injectedWrapperMethods.put(origin + "." + mw.getOriginalName() + mw.getOriginalDescription(), mw);
                    }
                }
                injected.addAll(classes);
                injectResources(IOUtils.readJarResources("/binaries/phantomshield-verification.bin"));
            } else {
                verifySoftwareId = -1L;
                verifyPublicKey = "";
            }
        } catch (Exception e) {
            ERROR("Request Verification Software ERROR");
            System.exit(1);
            return;
        }


        final ClassNode wrapper = new ClassNode();
        wrapper.version = V1_8;
        wrapper.access = ACC_PUBLIC;
        wrapper.superName = "java/lang/Object";
        wrapper.name = "tech/skidonion/verification/InlineWrapper";
//        ClassWrapper inline = injectClass(wrapper);
        ClassWrapper inline = new ClassWrapper(obfuscator, wrapper, false);
        AtomicInteger inlineIndex = new AtomicInteger();
        addInternalInclusion(wrapper.name, "*");
        getClassWrappers().forEach(classWrapper -> {
            final boolean classMatch = match(classWrapper);
            classWrapper.getMethods().forEach(methodWrapper -> {
                final boolean methodMatch = match(methodWrapper);
                final boolean obfuscated = classMatch && methodMatch;

                for (ListIterator<AbstractInsnNode> iterator = methodWrapper.getMethodNode().instructions.iterator(); iterator.hasNext(); ) {
                    AbstractInsnNode instruction = iterator.next();
                    if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                        String reference = methodInsnNode.owner + "." + methodInsnNode.name + methodInsnNode.desc;
                        switch (reference) {
                            case "tech/skidonion/obfuscator/inline/Inline._verification_checkHardwareID([Ljava/lang/Object;)V":
                            case "tech/skidonion/obfuscator/inline/Inline._verification_generateHardwareID([Ljava/lang/Object;)V": {
                                if (obfuscated) break;
                                MethodNode inlineMethod = new MethodNode();
                                inlineMethod.access = ACC_PUBLIC | ACC_STATIC;
                                inlineMethod.name = String.valueOf(inlineIndex.getAndIncrement());
                                inlineMethod.desc = methodInsnNode.desc;
                                Type[] arguments = Type.getArgumentTypes(methodInsnNode.desc);
                                for (int i = 0; i < arguments.length; i++) {
                                    Type argument = arguments[i];
                                    inlineMethod.instructions.add(new VarInsnNode(ASMUtils.getVarOpcode(argument, false), i));
                                }
                                inlineMethod.instructions.add(new MethodInsnNode(INVOKESTATIC, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, false));
                                inlineMethod.instructions.add(new InsnNode(ASMUtils.getReturnOpcode(Type.getReturnType(methodInsnNode.desc))));
                                iterator.remove();
                                inline.addMethod(inlineMethod);
                                iterator.add(new MethodInsnNode(INVOKESTATIC, inline.getOriginalName(), inlineMethod.name, inlineMethod.desc));
                                break;
                            }
                            case "tech/skidonion/obfuscator/inline/Inline._advanced_checkProtection(I)I":
                            case "tech/skidonion/obfuscator/inline/Inline._advanced_checkCRCImage(I)I":
                            case "tech/skidonion/obfuscator/inline/Inline._advanced_checkIsVirtualPC(I)I":
                            case "tech/skidonion/obfuscator/inline/Inline._advanced_checkIsDebuggerPresent(I)I": {
                                if (obfuscated) break;
                                MethodNode inlineMethod = new MethodNode();
                                inlineMethod.access = ACC_PUBLIC | ACC_STATIC;
                                inlineMethod.name = String.valueOf(inlineIndex.getAndIncrement());
                                iterator.previous();
                                AbstractInsnNode previous = iterator.previous();
                                int constant;
                                try {
                                    constant = ASMUtils.getIntegerFromInsn(previous);
                                } catch (Exception exception) {
                                    throw new RuntimeException("Advanced Inline Method need a const argument...");
                                }
                                iterator.remove();
                                iterator.next();
                                inlineMethod.desc = "()I";
                                inlineMethod.instructions.add(new LdcInsnNode(constant));
                                inlineMethod.instructions.add(new MethodInsnNode(INVOKESTATIC, methodInsnNode.owner, methodInsnNode.name, methodInsnNode.desc, false));
                                inlineMethod.instructions.add(new InsnNode(IRETURN));
                                iterator.remove();
                                inline.addMethod(inlineMethod);
                                iterator.add(new MethodInsnNode(INVOKESTATIC, inline.getOriginalName(), inlineMethod.name, inlineMethod.desc));
                                break;
                            }
                            case "tech/skidonion/verification/utils/Internals.verificationServer()Ljava/lang/String;": {
                                iterator.remove();
                                iterator.add(new LdcInsnNode(this.verification_server.getValue()));
                                break;
                            }
                            case "tech/skidonion/verification/utils/Internals.publicKey()Ljava/lang/String;": {
                                iterator.remove();
                                iterator.add(new LdcInsnNode(verifyPublicKey));
                                break;
                            }
                            case "tech/skidonion/verification/utils/Internals.softwareId()J": {
                                iterator.remove();
                                iterator.add(new LdcInsnNode(verifySoftwareId));
                                break;
                            }
                            case "tech/skidonion/obfuscator/inline/Wrapper.login(Ljava/lang/String;Ljava/lang/String;)I":
                            case "tech/skidonion/obfuscator/inline/Wrapper.setAsSuspected(Ljava/lang/String;)V":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getCloudConstant(II)Ljava/util/Optional;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getExpiredDate(Ljava/lang/String;)Ljava/util/Optional;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getExpiredDates()Ljava/util/Map;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.hasRole(Ljava/lang/String;)Z":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getUserId()J": {
                                iterator.remove();
                                iterator.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/verification/utils/VerifyUtils", methodInsnNode.name, methodInsnNode.desc, false));
                                break;
                            }
                        }
                    }
                }
            });
        });
        if (!inline.getMethods().isEmpty()) {
            obfuscator.classes.put(inline.getName(), inline);
            obfuscator.classpath.put(inline.getName(), inline);
            injected.add(inline);
        }

        if (!injected.isEmpty()) {
            Mapper mapper = new Mapper(obfuscator, injected);
            mapper.setRepackage(true);
            mapper.setRepakageName("/");
            mapper.generateMappings();
            mapper.apply();
        }
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

    public boolean isVerificationEnable() {
        return verification_enable.isEnable();
    }

    public boolean isUseInternalVerificationInterface() {
        return use_internal_user_interface.isEnable();
    }

}
