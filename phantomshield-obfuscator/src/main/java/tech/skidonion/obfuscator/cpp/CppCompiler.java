package tech.skidonion.obfuscator.cpp;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class CppCompiler {
    private final static SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd-hhmmss");
    private PhantomShield obfuscator;
    private boolean supportCrossCompile = false;
    private String compiler;
    private String extraCommandLine;
    private File outputDir;
    private boolean isAarch64 = false;
    private String defaultOutput = "x64-windows.dll";
    private final AtomicInteger virtualizeMacroCount = new AtomicInteger();
    private final List<String> targets = new ArrayList<>();
    private final List<String> cppFiles = new ArrayList<>();

    public CppCompiler(String compiler) {
        this.compiler = compiler;
    }

    public void init(PhantomShield obfuscator) {
        this.obfuscator = obfuscator;
        File compilerFile = new File(compiler);
        if (!compilerFile.exists()) {
            ERROR("compiler is not found");
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

    /*
    ${compiler_path} ${extra_command_line} -o ${output} ${inputs}
     */

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
                final File logfile_compile = new File("logs", "compile-" + target + "-" + timestamp + ".log");
                INFO("compiling with target: " + target + " [" + logfile_compile + "]");
                futures.add(PhantomShield.EXECUTOR.submit(() -> {
                    CompileInfo compileInfo = buildCompileInfo(target);

                    int compileValue = startProcess(makeCompileCommandLine(target, compileInfo), logfile_compile.getAbsoluteFile());
                    int strip = 0;
                    int virtualize = 0;
                    if (compileValue == 0 && compileInfo.getOs() == OS.MAC) {
                        final File logfile_strip = new File("logs", "strip-" + target + "-" + timestamp + ".log");
                        INFO("stripping debug information: " + target + " [" + logfile_strip + "]");
                        strip = startProcess(new String[]{"bin/llvm-strip.exe", "-s", "\"" + outputDir + "\\build\\" + compileInfo.output + "\""}, logfile_strip.getAbsoluteFile());
                    }
                    if (virtualizeMacroCount.get() > 0) {
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
            final File logfile = new File("logs/compile-" + timestamp + ".log");
            if (!logfile.getParentFile().exists()) logfile.getParentFile().mkdirs();
            INFO("compiling with target [" + logfile + "]");
            try {
                PhantomShield.EXECUTOR.submit(() -> {
                    int compileValue = startProcess(makeCompileCommandLine(null, null), logfile);
                    int virtualize = 0;
                    if (virtualizeMacroCount.get() > 0) {
                        virtualize = virtualize(timestamp, null);
                    }
                    return compileValue | virtualize;
                }).get();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        for (File file : Objects.requireNonNull(buildDir.listFiles())) {
            try (FileInputStream fis = new FileInputStream(file)) {
                obfuscator.resources.put(properties.get("loader_path") + "/" + file.getName(), IOUtils.toByteArray(fis));
            } catch (IOException e) {
                ERROR("inject native libraries failed:", e);
            }
        }
    }

    private int startProcess(String[] commands, File printFile) {
        try {
            Process process = new ProcessBuilder(commands)
                    .redirectErrorStream(true)
                    .redirectOutput(printFile)
                    .start();
            return process.waitFor();
        } catch (Exception e) {
            ERROR("compiling failed:", e);
        }
        return -1;
    }

    private int virtualize(String timestamp, CompileInfo compileInfo) {
        String output = compileInfo != null ? compileInfo.output : defaultOutput;
        File logfile_virtualize = new File("logs", "virtualize-" + output + "-" + timestamp + ".log");
        File origin = new File(outputDir + "\\build\\" + output);
        File newer = new File(outputDir + "\\build\\_" + output);
        INFO("virtualizing: " + output + " [" + logfile_virtualize + "]");
        String arch;
        if ((compileInfo != null && compileInfo.getArch() == ARCH.ARM64) || isAarch64) {
            arch = "bin/VirtualizerArm64.exe";
        } else {
            arch = "bin/Virtualizer.exe";
        }
        int virtualize = startProcess(new String[]{arch,
                        "/protect", "\"" + new File("bin/config.cv").getAbsoluteFile() + "\"",
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
                ERROR("project file does not exist or invalid.");
                break;
            case 2:
                ERROR("file to protect cannot be opened.");
                break;
            case 3:
                ERROR("file do not have any blocks to protect.");
                break;
            case 4:
                ERROR("error in inserted block.");
                break;
            case 5:
                ERROR("fatal error while protecting file.");
                break;
            case 6:
                ERROR("cannot write protected file to disk.");
                break;
            case 7:
                ERROR(output + " isn't compatible.");
                break;
            default:
                ERROR("unknown error");
                break;
        }
        return virtualize;
    }


    private String[] makeCompileCommandLine(String target, final CompileInfo compileInfo) {
        List<String> commands = new ArrayList<>();
        commands.add(String.format("\"%s\"", compiler));
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
                "-fPIC",
                "-Wno-narrowing"
        ));
        // TODO: if you wanna virtualize methods you must stop optimize your shit code
        //  because optimization will change control flow graph
        //  it will cause mistakes while virtualizing
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
    }

    private static CompileInfo buildCompileInfo(String target) {
        StringBuilder sb = new StringBuilder();
        ARCH arch;
        OS os;
        if (target.contains("x86_64") || target.contains("amd64")) {
            sb.append("x64");
            arch = ARCH.X64;
        } else if (target.contains("aarch64")) {
            sb.append("arm64");
            arch = ARCH.ARM64;
        } else if (target.contains("arm")) {
            sb.append("arm32");
            arch = ARCH.ARM32;
        } else if (target.contains("x86")) {
            sb.append("x86");
            arch = ARCH.X86;
        } else {
            sb.append("raw").append(target);
            arch = ARCH.RAW;
        }
        sb.append('-');
        if (target.contains("nix") || target.contains("nux") || target.contains("aix")) {
            sb.append("linux.so");
            os = OS.LINUX;
        } else if (target.contains("win")) {
            sb.append("windows.dll");
            os = OS.WINDOWS;
        } else if (target.contains("mac")) {
            sb.append("macos.dylib");
            os = OS.MAC;
        } else {
            sb.append("raw").append(target);
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
