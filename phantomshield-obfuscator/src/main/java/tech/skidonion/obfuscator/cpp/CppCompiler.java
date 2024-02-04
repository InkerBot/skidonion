package tech.skidonion.obfuscator.cpp;

import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.utils.IOUtils;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class CppCompiler {
    private PhantomShield obfuscator;
    private boolean supportCrossCompile = false;
    private String compiler;
    private String extraCommandLine;
    private File outputDir;
    private String defaultOutput = "x64-windows.dll";
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
            outputDir = new File(obfuscator.getConfig().getAsJsonPrimitive("output").getAsString()).getParentFile();
        File buildDir = new File(outputDir, "build");
        buildDir.mkdirs();

        if (supportCrossCompile) {
            if (targets.isEmpty()) {
                throw new RuntimeException("at least one target is required");
            }
            List<Future<Integer>> futures = new ArrayList<>();
            for (final String target : targets) {
                final File logfile = new File("compile_" + target + "_" + RandomUtils.getRandomLetters(8) + ".log");
                INFO("compiling with target: " + target + "...\nlog file: " + logfile);
                futures.add(PhantomShield.EXECUTOR.submit(() -> startProcess(makeCompileCommandLine(target), logfile)));
            }
            futures.forEach(f -> {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
        } else {
            final File logfile = new File("compile_" + RandomUtils.getRandomLetters(8) + ".log");
            INFO("compiling with default target...\nlog file: " + logfile);
            try {
                PhantomShield.EXECUTOR.submit(() -> startProcess(makeCompileCommandLine(null), logfile)).get();
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
            e.printStackTrace();
        }
        return -1;
    }


    private String[] makeCompileCommandLine(String target) {
        final CompileInfo compileInfo = supportCrossCompile && target != null ? buildCompileInfo(target) : null;

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
                "-l", "c",
                "-O2",
                "-s",
                "-fno-sanitize=all",
                "-fno-sanitize-trap=all",
                "-fno-optimize-sibling-calls",
                "-fvisibility-inlines-hidden",
                "-fvisibility=hidden",
                "-fPIC",
                "-Wno-narrowing"
        ));
        if (compileInfo != null) {
            if (compileInfo.getOs() == OS.MAC) {
                commands.add("-Wl,-headerpad_max_install_names");
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
