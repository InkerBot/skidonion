package tech.skidonion.obfuscator.cpp;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.*;
import static tech.skidonion.obfuscator.utils.StringUtils.createStringMap;

public class CppCompiler {
    private final static SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd-hhmmss");
    private PhantomShield obfuscator;
    private boolean supportCrossCompile = false;
    private String compiler;
    private String extraCommandLine;
    private File outputDir;
    private boolean isDebug = false;
    private int isOnlyIntelPE = -1;
    private boolean isAarch64 = false;
    private String defaultOutput = "x64-windows.dll";
    private final AtomicInteger virtualizeMacroCount = new AtomicInteger();
    private final List<String> targets = new ArrayList<>();
    private final List<String> cppFiles = new ArrayList<>();
    //    private boolean legacyCompileMode = false;
    private static final boolean legacyCompileMode = true;

    public CppCompiler(String compiler) {
        this.compiler = compiler;
    }

    public void init(PhantomShield obfuscator) {
        this.obfuscator = obfuscator;
        this.outputDir = new File(obfuscator.getConfig().getString("output")).getParentFile();

        if (obfuscator.getConfig().has("__debug")) {
            this.isDebug = obfuscator.getConfig().getBoolean("__debug");
        }

        File compilerFile = new File(compiler);
        if (!compilerFile.exists()) {
            ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.compile"));
            this.compiler = CompilerUpdater.DEFAULT_COMPILER;
        }
        if (CompilerUpdater.DEFAULT_COMPILER.equals(this.compiler)) {
            this.supportCrossCompile = true;
            File zig = new File(CompilerUpdater.DEFAULT_COMPILER);
            if (!zig.exists()) {
                CompilerUpdater.updateCompiler();
            }
        }
    }

    public void addCppFile(String file) {
        cppFiles.add(file);
    }

    public void compile(Map<String, String> properties) {
        if (outputDir == null)
            outputDir = new File(obfuscator.getConfig().getString("output")).getParentFile();
        File buildDir = new File(outputDir, "build");
        buildDir.mkdirs();

        String timestamp = FORMATTER.format(new Date());

        if (supportCrossCompile) {
            if (targets.isEmpty()) {
                throw new RuntimeException("at least one target is required");
            }
            List<Future<Integer>> futures = new ArrayList<>();
            for (final String target : targets) {
                final File logfile_compile = new File("logs/" + target + "/" + timestamp + ".log");
                if (!logfile_compile.getParentFile().exists()) logfile_compile.getParentFile().mkdirs();
                INFO(TRANSLATION("phantom-shield-x.cpp-compiler.compile3"), target, logfile_compile);
                futures.add(PhantomShield.EXECUTOR.submit(() -> {
                    CompileInfo compileInfo = buildCompileInfo(target);

                    int compileValue = startProcess(makeCompileCommandLine(target, compileInfo), logfile_compile.getAbsoluteFile(), "ZIG_LOCAL_CACHE_DIR", Paths.get(".cache").toAbsolutePath().toString());
                    int strip = 0;
                    int virtualize = 0;
                    if (compileValue == 0 && compileInfo.getOs() == OS.MAC) {
                        final File logfile_strip = new File("logs/strip/" + target + "/" + timestamp + ".log");
                        if (!logfile_strip.getParentFile().exists()) logfile_strip.getParentFile().mkdirs();
                        INFO(TRANSLATION("phantom-shield-x.cpp-compiler.strip"), target, logfile_strip);
                        strip = startProcess(new String[]{"bin/llvm-strip.exe", "-s", "\"" + outputDir + "\\build\\" + compileInfo.output + "\""}, logfile_strip.getAbsoluteFile());
                    }
                    if (virtualizeMacroCount.get() > 0 && !isDebug) {
                        virtualize = virtualize(timestamp, compileInfo);
                    }
                    return compileValue | strip | virtualize;
                }));
            }
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            final File logfile = new File("logs/compile/" + timestamp + ".log");
            if (!logfile.getParentFile().exists()) logfile.getParentFile().mkdirs();
            INFO(TRANSLATION("phantom-shield-x.cpp-compiler.compile4"), logfile);
            try {
                PhantomShield.EXECUTOR.submit(() -> {
                    int compileValue = startProcess(makeCompileCommandLine(null, null), logfile, "ZIG_LOCAL_CACHE_DIR", Paths.get(".cache").toAbsolutePath().toString());
                    int virtualize = 0;
                    if (virtualizeMacroCount.get() > 0 && !isDebug) {
                        virtualize = virtualize(timestamp, null);
                    }
                    return compileValue | virtualize;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        injectLibrary(buildDir, properties);

    }

    private void injectLibrary(File dir, Map<String, String> properties) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    this.injectLibrary(file, properties);
                } else {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        obfuscator.resources.put(properties.get("loader_path") + "/" + file.getName(), IOUtils.toByteArray(fis));
                    } catch (IOException e) {
                        ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.inject"), e);
                    }
                }
            }
        }
    }

    public boolean isAdvancedModuleEnable() {
        if (isOnlyIntelPE == -1) {
            if (!supportCrossCompile) {
                isOnlyIntelPE = 0;
                return false;
            }
            isOnlyIntelPE = 1;
            for (String target : this.targets) {
                CompileInfo info = buildCompileInfo(target);
                if (info.getOs() != OS.WINDOWS || (info.getArch() != ARCH.X64 && info.getArch() != ARCH.X86)) {
                    isOnlyIntelPE = 0;
                    return false;
                }
            }
        }
        return isOnlyIntelPE == 1;
    }

    private int startProcess(String[] commands, File printFile, String... env) {
        try {
            ProcessBuilder builder = new ProcessBuilder(commands);
            builder.environment().putAll(createStringMap((Object[]) env));
            Process process = builder
                    .redirectErrorStream(true)
                    .redirectOutput(printFile)
                    .start();
            return process.waitFor();
        } catch (Exception e) {
            ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.compile5"), e);
        }
        return -1;
    }

    private int virtualize(String timestamp, CompileInfo compileInfo) {
        String output = compileInfo != null ? compileInfo.output : defaultOutput;
        File logfile_virtualize = new File("logs/virtualize/" + output + "/" + timestamp + ".log");
        if (!logfile_virtualize.getParentFile().exists()) logfile_virtualize.getParentFile().mkdirs();
        File origin = new File(outputDir + "\\build\\" + output);
        if (!origin.exists()) {
            origin = new File(outputDir + "\\build\\bin\\" + output);
        }
        File newer = new File(outputDir + "\\build\\_" + output);
        INFO(TRANSLATION("phantom-shield-x.cpp-compiler.virtualize"), output, logfile_virtualize);
        String arch;
        File config;
        if (isAdvancedModuleEnable()) {
            config = new File("bin/config.tmd").getAbsoluteFile();
            if (compileInfo != null && compileInfo.getArch() == ARCH.X64) {
                arch = "bin/Themida64.exe";
            } else {
                arch = "bin/Themida.exe";
            }
        } else {
            config = new File("bin/config.cv").getAbsoluteFile();
            if ((compileInfo != null && compileInfo.getArch() == ARCH.ARM64) || isAarch64) {
                arch = "bin/VirtualizerArm64.exe";
            } else {
                arch = "bin/Virtualizer.exe";
            }
        }
        int virtualize = startProcess(new String[]{arch,
                        "/protect", "\"" + config + "\"",
                        "/inputfile", "\"" + origin.getAbsoluteFile() + "\"",
                        "/outputfile", "\"" + newer.getAbsoluteFile() + "\""
                },
                logfile_virtualize.getAbsoluteFile());
        switch (virtualize) {
            case 0:
                origin.delete();
                newer.renameTo(origin);
                break;
            case 1:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.project"));
                break;
            case 2:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.file"));
                break;
            case 3:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.file2"));
                break;
            case 4:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.error"));
                break;
            case 5:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.fatal"));
                break;
            case 6:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.cannot"));
                break;
            case 7:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.output"), output);
                break;
            default:
                ERROR(TRANSLATION("phantom-shield-x.cpp-compiler.unknown"));
                break;
        }
        return virtualize;
    }


    private String[] makeCompileCommandLine(String target, final CompileInfo compileInfo) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(String.format("\"%s\"", compiler));

        if (legacyCompileMode || target == null) {
            if (compileInfo != null) {
                commands.add("c++");
                commands.add("-target");
                commands.add(target);
            }
            if (extraCommandLine != null) commands.add(extraCommandLine);
            commands.addAll(Arrays.asList(
                    "-std=c++17",
                    "-shared",
                    "-I", String.format("\"%s\"", outputDir),
                    "-L", String.format("\"%s\"", outputDir),
                    "-l", "c++",
                    "-s",
                    "-fno-sanitize=all",
                    "-fno-sanitize-trap=all",
                    "-fno-optimize-sibling-calls",
                    "-fvisibility-inlines-hidden",
                    "-fvisibility=hidden",
                    "-fPIC"
            ));
            // TODO: if you wanna virtualize methods you must stop optimize your shit code
            //  because optimization will change control flow graph
            //  it will cause mistakes while virtualizing
            commands.add("-DNDEBUG");
            if (virtualizeMacroCount.get() == 0) {
                commands.add("-O2");
            }
            if (compileInfo != null) {
                if (compileInfo.getOs() == OS.MAC) {
                    commands.add("-Wl,-headerpad_max_install_names");
                    // TODO: why can't strip symbols for macos???(resolved via use llvm-strip.exe)
                    commands.add("-Wl,-s");
                }
            }
            commands.add("-o");
            if (compileInfo != null) {
                commands.add("\"" + outputDir + "\\build\\" + compileInfo.output + "\"");
            } else {
                commands.add("\"" + outputDir + "\\build\\" + defaultOutput + "\"");
            }
            commands.addAll(cppFiles);
            return commands.toArray(new String[0]);
        } else {
            ZigBuildFileBuilder zigBuildFileBuilder = new ZigBuildFileBuilder();
            cppFiles.forEach(path -> zigBuildFileBuilder.addCppFile(path.substring(outputDir.getAbsoluteFile().toString().length() + 1)));
            Path build_zig = outputDir.toPath().resolve("build.zig");
            Files.write(build_zig, zigBuildFileBuilder.build().getBytes(StandardCharsets.UTF_8));
            commands.add("build");
            commands.add("-Dtarget=" + target);
            String suffix;
            switch (compileInfo.arch) {
                case X64:
                    suffix = "64";
                    break;
                case X86:
                    suffix = "32";
                    break;
                case ARM64:
                    suffix = "ARM64";
                    break;
                default:
                    suffix = "";
                    break;
            }
            commands.add("-Doutput_name=PhantomShieldX" + suffix);
            commands.add("-p");
            commands.add(String.format("\"%s\"", outputDir.toPath().resolve("build")));
            commands.add("--prefix-lib-dir");
            commands.add("\"./\"");
            commands.add("--build-file");
            commands.add(String.format("\"%s\"", build_zig.toAbsolutePath()));
            if (virtualizeMacroCount.get() == 0) {
                commands.add("-Doptimize=ReleaseFast");
            }
            return commands.toArray(new String[0]);
        }
    }

    private static CompileInfo buildCompileInfo(String target) {
        StringBuilder sb = new StringBuilder("PhantomShieldX");
        ARCH arch;
        OS os;
        if (target.contains("x86_64") || target.contains("amd64")) {
            sb.append("64");
            arch = ARCH.X64;
        } else if (target.contains("aarch64")) {
            sb.append("ARM64");
            arch = ARCH.ARM64;
        } else if (target.contains("arm")) {
            arch = ARCH.ARM32;
        } else if (target.contains("x86")) {
            sb.append("32");
            arch = ARCH.X86;
        } else {
            arch = ARCH.RAW;
        }
        if (target.contains("nix") || target.contains("nux") || target.contains("aix")) {
            sb.insert(0, "lib");
            sb.append(".so");
            os = OS.LINUX;
        } else if (target.contains("win")) {
            sb.append(".dll");
            os = OS.WINDOWS;
        } else if (target.contains("mac")) {
            sb.insert(0, "lib");
            sb.append(".dylib");
            os = OS.MAC;
        } else {
            sb.insert(0, "lib");
            sb.append(".so");
            os = OS.RAW;
        }

        return new CompileInfo(os, arch, sb.toString());
    }

    public void addTarget(String target) {
        this.targets.add(target);
    }

    public void addTargets(String... targets) {
        this.targets.addAll(Arrays.asList(targets));
    }

    public void setExtraCommandLine(String extraCommandLine) {
        this.extraCommandLine = extraCommandLine;
    }

    /*
     * used for a compiled library
     * */
    public void setDefaultOutput(String defaultOutput) {
        this.defaultOutput = defaultOutput;
    }

    public void setOutputDir(File outputDir) {
        this.outputDir = outputDir;
    }

    public AtomicInteger getVirtualizeMacroCount() {
        return virtualizeMacroCount;
    }

    public void setAarch64(boolean aarch64) {
        isAarch64 = aarch64;
    }

//    public void setLegacyCompileMode(boolean value) {
//        legacyCompileMode = value;
//    }

    static class CompileInfo {
        private final OS os;
        private final ARCH arch;
        private final String output;

        public CompileInfo(OS os, ARCH arch, String output) {
            this.os = os;
            this.arch = arch;
            this.output = output;
        }

        public OS getOs() {
            return os;
        }

        public ARCH getArch() {
            return arch;
        }

        public String getOutput() {
            return output;
        }
    }

    enum OS {
        WINDOWS("windows"),
        LINUX("linux"),
        MAC("macos"),
        RAW("raw");
        private final String name;

        OS(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    enum ARCH {
        X86("x86"),
        X64("x64"),
        ARM32("arm32"),
        ARM64("arm64"),
        RAW("raw");
        private final String name;

        ARCH(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}
