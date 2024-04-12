package tech.skidonion.obfuscator.transformer.impl;

import com.google.gson.JsonObject;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.CustomClassWriter;
import tech.skidonion.obfuscator.asm.FieldWrapper;
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
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.internals.*;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.snippets.Snippets;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.ClassSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.InlineSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.MainSourceBuilder;
import tech.skidonion.obfuscator.transformer.impl.nativeobfuscation.source.StringPool;
import tech.skidonion.obfuscator.transformer.impl.renamer.Mapper;
import tech.skidonion.obfuscator.utils.*;
import tech.skidonion.obfuscator.utils.commons.Pair;
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

import static tech.skidonion.obfuscator.PhantomShield.*;

public class NativeObfuscation extends Transformer {
    public static final String INLINE_DESC = Type.getDescriptor(tech.skidonion.obfuscator.annotations.NativeObfuscation.Inline.class);
    public final Map<String, MethodWrapper> injectedWrapperMethods = new HashMap<>();
    public final Map<String, Pair<String, FieldWrapper>> inlineFields = new HashMap<>();
    private final BooleanValue print_instructions = new BooleanValue("print_instructions", false);
    private final ClassPackageValue loader_package = new ClassPackageValue("loader_package", "skidonion/??????");
    private final BooleanValue hidden_stack_trace = new BooleanValue("hidden_stack_trace", true);

    private final BooleanValue verification_enable = new BooleanValue("verification_enable", false);
    private final BooleanValue use_internal_user_interface = new BooleanValue("use_internal_user_interface", true);
    private final StringValue verification_server = new StringValue("verification_server", "https://skidonion.tech/");
    private final StringValue verification_user_id = new StringValue("verification_user_id", "-1");
    private final StringValue verification_software_id = new StringValue("verification_software_id", "-1");
    private final StringValue verification_token = new StringValue("verification_token", "");
    private final SubValue verification = new SubValue("verification", verification_enable, use_internal_user_interface, verification_server, verification_user_id, verification_software_id, verification_token);

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

        Optional<String> opt = Wrapper.getCloudConstant(271423823, 0);

        final boolean[] internalTip = {false};

        Integer[] classIndexReference = new Integer[]{0};
        getFilteredClasses().forEach(cw -> {
            boolean clinitIgnoreTryCatch = false;
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
                    Object ignoreTryCatch = map.get("ignoreTryCatch");
                    if (ignoreTryCatch instanceof Boolean) {
                        clinitIgnoreTryCatch = (boolean) ignoreTryCatch;
                    }
                }
            }
            try {
                addInternalInclusion(cw.getOriginalName(), "<clinit>()V");
                cw.getOrCreateClinit();
                StringBuilder nativeMethods = new StringBuilder();
                List<HiddenCppMethod> hiddenMethods = new ArrayList<>();
                String displayName = cw.getOriginalName();
                boolean isInternal = displayName.startsWith("iterator");
                if (isInternal) {
                    if (!internalTip[0]) {
                        displayName = "[Internal Classes]";
                        internalTip[0] = true;
                    } else {
                        displayName = null;
                    }
                }

                if (displayName != null) INFO(TRANSLATION("phantom-shield-x.native.covert"), displayName);

                cw.getMethods().stream().filter(this::match)
                        .map(MethodWrapper::getMethodNode)
                        .filter(MethodProcessor::shouldProcess)
                        .forEach(PreprocessorRunner::preprocess);

                CustomClassWriter computedWriter = new CustomClassWriter(Opcodes.ASM9 | ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES, obfuscator);
                cw.getClassNode().accept(computedWriter);

                ClassReader computedReader = new ClassReader(computedWriter.toByteArray());
                ClassNode computedClassNode = new ClassNode(Opcodes.ASM9);
                computedReader.accept(computedClassNode, 0);
                if (opt.isPresent() && (Integer.parseInt(opt.get()) ^ 1825605542) == 1789160537) {
                    IntStream.range(0, computedClassNode.methods.size())
                            .forEach(i -> cw.getMethods().get(i).setMethodNode(computedClassNode.methods.get(i)));
                    IntStream.range(0, computedClassNode.fields.size())
                            .forEach(i -> cw.getFields().get(i).setFieldNode(computedClassNode.fields.get(i)));
                }

                cw.setClassNode(computedClassNode);

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
                            Object ignoreTryCatch = map.get("ignoreTryCatch");
                            if (ignoreTryCatch instanceof Boolean) {
                                context.ignoreTryCatch = (boolean) ignoreTryCatch;
                            }
                        }
                        if ("<clinit>".equals(method.getName())) {
                            if (!"NONE".equals(clinitVirtualization)) {
                                shouldVirtualize = true;
                                context.virtualization = clinitVirtualization;
                            }
                            context.ignoreTryCatch = clinitIgnoreTryCatch;
                        }
                        if (opt.isPresent() && (Integer.parseInt(opt.get()) ^ 1825605542) == 1789160537)
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
                ERROR(TRANSLATION("phantom-shield-x.native.error"), cw.getOriginalName(), ex);
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

        inlineSourceBuilder.buildHeader();
        inlineSourceBuilder.buildInlineFields();
        inlineSourceBuilder.buildVerificationField();
        inlineSourceBuilder.buildInjectInlines();
        inlineSourceBuilder.buildTail();

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
        long last = System.currentTimeMillis();
        INFO(TRANSLATION("phantom-shield-x.native.preprocess"));
        this.init();

        String loaderClassName = nativeDir + "/___";

        ClassNode loaderClass;

        List<ClassNode> classNodes = ASMUtils.readClassesWithInputStream("/binaries/phantomshield-loader.bin", 0);
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
        Optional<String> opt = Wrapper.getCloudConstant(467287013, 0);

        List<ClassWrapper> injected = new ArrayList<>();
        long verifySoftwareId;
        String verifyPublicKey;
        String verifyVersion;
        if (isVerificationEnable() && opt.isPresent() && (Integer.parseInt(opt.get()) ^ 173359771) == 2082061244) {
            JsonObject softwareInformation = VerifyUtils.requestSoftwareInformation(this.verification_server.getValue(), this.verification_user_id.getValue(), this.verification_token.getValue(), this.verification_software_id.getValue());
            if (softwareInformation == null || softwareInformation.getAsJsonPrimitive("code").getAsLong() != 0L) {
                ERROR(TRANSLATION("phantom-shield-x.native.request"));
                System.exit(0);
                return;
            }
            JsonObject entity = softwareInformation.getAsJsonObject("entity");
            verifySoftwareId = entity.getAsJsonPrimitive("id").getAsLong();
            verifyPublicKey = entity.getAsJsonPrimitive("public_key").getAsString();
            verifyVersion = entity.getAsJsonPrimitive("version").getAsString();
            INFO(TRANSLATION("phantom-shield-x.native.software"), entity.getAsJsonPrimitive("software_name").getAsString());
            List<ClassWrapper> classes = injectClasses(ASMUtils.readClassesWithInputStream("/binaries/phantomshield-verification.bin", ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES));
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
            {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(VerifyUtilsDump.dump());
                reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.add(injectClass(node));
            }
            {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(URLEncoderDump.dump());
                reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.add(injectClass(node));
            }
            {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(EdDSAEngineDump.dump());
                reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.add(injectClass(node));
            }
            {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(ChaCha20Dump.dump());
                reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.add(injectClass(node));
            }
            {
                ClassNode node = new ClassNode();
                ClassReader reader = new ClassReader(Base64Dump.dump());
                reader.accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                classes.add(injectClass(node));
            }
            for (ClassWrapper cw : classes) {
                obfuscator.buildHierarchy(cw, null);
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
            verifyVersion = "";
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
        AtomicInteger inlineFieldIndex = new AtomicInteger();
        getClassWrappers().forEach(classWrapper -> {
            boolean isCloseable;
            if (classWrapper.getInterfaces() != null) {
                Set<String> interfaces = new HashSet<>(classWrapper.getInterfaces());
                isCloseable = interfaces.contains("java/lang/AutoCloseable") || interfaces.contains("java/io/Closeable");
            } else {
                isCloseable = false;
            }

            Set<String> inlineVirtualFields = new HashSet<>();
            int i = 0;
            for (Iterator<FieldWrapper> iterator = classWrapper.getFields().iterator(); iterator.hasNext(); i++) {
                FieldWrapper fieldWrapper = iterator.next();
                if (ASMUtils.hasAnnotation(fieldWrapper, INLINE_DESC)) {
                    String key = fieldWrapper.getOwner().getName() + "." + fieldWrapper.getName() + "." + fieldWrapper.getDescription();
                    if (!fieldWrapper.getAccess().isStatic()) {
                        inlineVirtualFields.add(key);
                    }
                    inlineFields.put(key, new Pair<>("__phantom_shield_x_" + StringUtils.escapeCppNameString(fieldWrapper.getName().replace('/', '_')) + inlineFieldIndex.getAndIncrement(), fieldWrapper));
                    iterator.remove();
                    classWrapper.getClassNode().fields.remove(i--);
                }
            }

            boolean shouldAddGarbageCollection = isCloseable && !inlineVirtualFields.isEmpty();
            if (shouldAddGarbageCollection) {
                addInternalInclusion(classWrapper.getOriginalName(), "close()V");
            }

            i = 0;
            for (Iterator<MethodWrapper> iterator = classWrapper.getMethods().iterator(); iterator.hasNext(); i++) {
                MethodWrapper methodWrapper = iterator.next();
                if (shouldAddGarbageCollection && Objects.equals("close()V", methodWrapper.getOriginalName() + methodWrapper.getOriginalDescription())) {
                    InsnList instructions = methodWrapper.getInstructions();
                    for (String inlineVirtualField : inlineVirtualFields) {
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(ALOAD, 0));
                        insnList.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_field_" + inlineVirtualField, "(Ljava/lang/Object;)V", false));
                        instructions.insert(insnList);
                    }
                }
            }
        });
        getClassWrappers().forEach(classWrapper -> {
            final boolean classMatch = match(classWrapper);
            classWrapper.getMethods().forEach(methodWrapper -> {
                final boolean methodMatch = match(methodWrapper);
                final boolean obfuscated = classMatch && methodMatch;

                for (ListIterator<AbstractInsnNode> iterator = methodWrapper.getMethodNode().instructions.iterator(); iterator.hasNext(); ) {
                    AbstractInsnNode instruction = iterator.next();
                    if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode fieldInsnNode = (FieldInsnNode) instruction;
                        String key = fieldInsnNode.owner + "." + fieldInsnNode.name + "." + fieldInsnNode.desc;
                        Pair<String, FieldWrapper> pair = inlineFields.get(key);
                        if (pair != null) {
                            int opcode = instruction.getOpcode();
                            MethodInsnNode injectedNode = null;
                            if (opcode == GETSTATIC) {
                                iterator.remove();
                                injectedNode = new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_field_" + key, "()" + fieldInsnNode.desc, false);
                            } else if (opcode == PUTSTATIC) {
                                iterator.remove();
                                injectedNode = new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_field_" + key, "(" + fieldInsnNode.desc + ")V", false);
                            } else if (opcode == GETFIELD) {
                                iterator.remove();
                                injectedNode = new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_field_" + key, "(Ljava/lang/Object;)" + fieldInsnNode.desc, false);
                            } else if (opcode == PUTFIELD) {
                                iterator.remove();
                                injectedNode = new MethodInsnNode(INVOKESTATIC, "tech/skidonion/obfuscator/inline/Inline", "_field_" + key, "(Ljava/lang/Object;" + fieldInsnNode.desc + ")V", false);
                            }

                            if (injectedNode != null) {
                                if (obfuscated) {
                                    iterator.add(injectedNode);
                                } else {
                                    MethodNode inlineMethod = new MethodNode();
                                    inlineMethod.access = ACC_PUBLIC | ACC_STATIC;
                                    inlineMethod.name = String.valueOf(inlineIndex.getAndIncrement());
                                    inlineMethod.desc = injectedNode.desc;
                                    Type[] arguments = Type.getArgumentTypes(injectedNode.desc);
                                    for (int i = 0; i < arguments.length; i++) {
                                        Type argument = arguments[i];
                                        inlineMethod.instructions.add(new VarInsnNode(ASMUtils.getVarOpcode(argument, false), i));
                                    }
                                    inlineMethod.instructions.add(injectedNode);
                                    inlineMethod.instructions.add(new InsnNode(ASMUtils.getReturnOpcode(Type.getReturnType(injectedNode.desc))));
                                    inline.addMethod(inlineMethod);
                                    iterator.add(new MethodInsnNode(INVOKESTATIC, inline.getOriginalName(), inlineMethod.name, inlineMethod.desc));
                                }
                            }
                        }

                    } else if (instruction instanceof MethodInsnNode) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) instruction;
                        String reference = methodInsnNode.owner + "." + methodInsnNode.name + methodInsnNode.desc;
                        switch (reference) {
                            case "tech/skidonion/obfuscator/inline/Inline._verification_checkHardwareID([Ljava/lang/Object;)V":
                            case "tech/skidonion/obfuscator/inline/Inline._verification_generateHardwareID([Ljava/lang/Object;)V": {
//                                2082061244
//                                173359771
//                                1984756007
                                if (obfuscated || !opt.isPresent() || (Integer.parseInt(opt.get()) ^ 173359771) != 2082061244)
                                    break;
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
                            case "tech/skidonion/obfuscator/inline/Inline.trycatch()V": {
                                if (!obfuscated) {
                                    ERROR(TRANSLATION("phantom-shield-x.native.trycatch"));
                                    System.exit(0);
                                    return;
                                }
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
                            case "tech/skidonion/verification/utils/Internals.version()Ljava/lang/String;": {
                                iterator.remove();
                                iterator.add(new LdcInsnNode(verifyVersion));
                                break;
                            }
                            case "tech/skidonion/obfuscator/inline/Wrapper.getVerifyToken()Ljava/lang/String;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.login(Ljava/lang/String;Ljava/lang/String;)I":
                            case "tech/skidonion/obfuscator/inline/Wrapper.setAsSuspected(Ljava/lang/String;)V":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getCloudConstant(II)Ljava/util/Optional;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getExpiredDate(Ljava/lang/String;)Ljava/util/Optional;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getExpiredDates()Ljava/util/Map;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.hasRole(Ljava/lang/String;)Z":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getUsername()Ljava/util/Optional;":
                            case "tech/skidonion/obfuscator/inline/Wrapper.getUserId()J": {
                                if (!opt.isPresent() || (Integer.parseInt(opt.get()) ^ 173359771) != 2082061244)
                                    break;
                                iterator.remove();
                                iterator.add(new MethodInsnNode(INVOKESTATIC, "tech/skidonion/verification/utils/VerifyUtils", methodInsnNode.name, methodInsnNode.desc, false));
                                break;
                            }
                            case "tech/skidonion/obfuscator/inline/Wrapper._debug_addDefaultCloudConstant(Ljava/lang/String;Ljava/lang/String;)V":
                                ERROR(TRANSLATION("phantom-shield-x.native.you"));
                                System.exit(0);
                                break;
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
            Collections.shuffle(injected);
            Renamer renamer = (Renamer) obfuscator.getRegister().get("renamer");
            Mapper mapper = new Mapper(obfuscator, injected);
            mapper.setRepackage(true);
            mapper.setPrefixName(renamer.prefix_name.getValue());
            mapper.setRepakageName(renamer.repackage_name.getValue());
            mapper.generateMappings();
            mapper.apply();
        }
        INFO(TRANSLATION("phantom-shield-x.native.preprocess2"), System.currentTimeMillis() - last);
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
