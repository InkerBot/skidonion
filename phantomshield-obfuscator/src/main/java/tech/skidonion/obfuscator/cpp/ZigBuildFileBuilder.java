package tech.skidonion.obfuscator.cpp;


public class ZigBuildFileBuilder {
    private StringBuilder zig = new StringBuilder();
    private StringBuilder cppFiles = new StringBuilder();


    public void addCppFile(String cppFilePath) {
        this.cppFiles
                .append("        \"")
                .append(cppFilePath.replace("\\", "\\\\"))
                .append("\",\n");
    }

    public String build() {
        zig
                .append("const std = @import(\"std\");\npub fn build(b: *std.Build) void {\n")
                .append("    const target = b.standardTargetOptions(.{});\n")
                .append("    const optimize = b.standardOptimizeOption(.{});\n")
                .append("\n")
                .append("    const name = b.option([]const u8, \"output_name\", \"output_name\") orelse \"PhantomShieldX\";\n")
                .append("    const lib = b.addSharedLibrary(.{\n")
                .append("        .name = name,\n")
                .append("        .strip = true,\n")
                .append("        .target = target,\n")
                .append("        .optimize = optimize,\n")
                .append("    });\n")
                .append("    lib.addIncludePath(.{ .path = \"./\" });\n")
                .append("    lib.addIncludePath(.{ .path = \"./output/\" });\n")
                .append("\n")
                .append("    const cpp_files = .{\n")
                .append(cppFiles.toString())
                .append("    };\n")
                .append("\n")
                .append("    const cpp_flags = .{\n")
                .append("        \"-std=c++17\",\n")
                .append("        \"-stdlib=libc++\",\n")
                .append("        \"-fno-exceptions\",\n")
                .append("        \"-fno-sanitize=all\",\n")
                .append("        \"-fno-sanitize-trap=all\",\n")
                .append("        \"-fno-optimize-sibling-calls\",\n")
                .append("        \"-fvisibility-inlines-hidden\",\n")
                .append("        \"-fvisibility=hidden\",\n")
                .append("        \"-fPIC\",\n")
                .append("        \"-DNDEBUG\",\n")
                .append("    };\n")
                .append("\n")
                .append("    switch (target.result.os.tag) {\n")
                .append("        .macos => {\n")
                .append("            const flags = cpp_flags ++ .{\n")
                .append("                \"-Wl,-headerpad_max_install_names\",\n")
                .append("                \"-Wl,-s\",\n")
                .append("            };\n")
                .append("            lib.addCSourceFiles(.{\n")
                .append("                .files = &cpp_files,\n")
                .append("                .flags = &flags,\n")
                .append("            });\n")
                .append("        },\n")
                .append("        else => {\n")
                .append("            lib.addCSourceFiles(.{\n")
                .append("                .files = &cpp_files,\n")
                .append("                .flags = &cpp_flags,\n")
                .append("            });\n")
                .append("        },\n")
                .append("    }\n")
                .append("\n")
                .append("    lib.linkLibCpp();\n")
                .append("    b.installArtifact(lib);\n")
                .append("}");
        return zig.toString();
    }

}
