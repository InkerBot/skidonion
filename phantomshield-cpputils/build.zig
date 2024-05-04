const std = @import("std");

pub fn build(b: *std.Build) void {
    const targets: []const std.Target.Query = &.{
        .{ .cpu_arch = .aarch64, .os_tag = .macos },
        .{ .cpu_arch = .aarch64, .os_tag = .linux, .abi = .gnu },
        .{ .cpu_arch = .x86_64, .os_tag = .linux, .abi = .gnu },
        .{ .cpu_arch = .x86, .os_tag = .linux, .abi = .gnu },
        .{ .cpu_arch = .x86_64, .os_tag = .windows },
        .{ .cpu_arch = .x86, .os_tag = .windows },
        .{ .cpu_arch = .aarch64, .os_tag = .windows },
    };

    const optimize = b.standardOptimizeOption(.{ .preferred_optimize_mode = .ReleaseSmall });
    for (targets) |target| {
        var buffer = [_]u8{undefined} ** 100;
        const t = target.os_tag orelse unreachable;

        const a = target.cpu_arch orelse unreachable;
        const name = if (target.abi) |value|
            std.fmt.bufPrint(&buffer, "JNI-Recode-{s}-{s}-{s}", .{ @tagName(t), std.Target.Os.Tag.archName(t, a), @tagName(value) }) catch |err| {
                std.debug.print("Error: {}\n", .{err});
                return;
            }
        else
            std.fmt.bufPrint(&buffer, "JNI-Recode-{s}-{s}", .{ @tagName(t), std.Target.Os.Tag.archName(t, a) }) catch |err| {
                std.debug.print("Error: {}\n", .{err});
                return;
            };
        // const name = std.fmt.bufPrint(&buffer, "JNI-Recode-{s}-{s}={s}", .{ @tagName(t), std.Target.Os.Tag.archName(t, a), abiName }) catch |err| {
        //     std.debug.print("Error: {}\n", .{err});
        //     return;
        // };
        const lib = b.addStaticLibrary(.{
            .name = name,
            .strip = true,
            .target = b.resolveTargetQuery(target),
            .optimize = optimize,
        });
        lib.addIncludePath(.{ .path = "./" });
        lib.addIncludePath(.{ .path = "./include" });

        const cpp_files = .{
            "source/baieroops.cpp",
            "source/jvm_internal.cpp",
            "source/jvm_static.cpp",
            "source/symbol.cpp",
            "source/array.cpp",
            "source/const_pool.cpp",
            "source/field_info.cpp",
            "source/JNIId.cpp",
        };

        const cpp_flags = .{
            "-std=c++20",
            "-stdlib=libc++",
            "-fno-exceptions",
            "-fno-sanitize=all",
            "-fno-sanitize-trap=all",
            "-fno-optimize-sibling-calls",
            "-fvisibility-inlines-hidden",
            "-fvisibility=hidden",
            "-fPIC",
            "-DNDEBUG",
        };
        lib.linkLibCpp();
        lib.linkLibC();
        switch (t) {
            .macos => {
                const flags = cpp_flags ++ .{
                    "-Wl,-headerpad_max_install_names",
                    "-Wl,-s",
                };
                lib.addCSourceFiles(.{
                    .files = &cpp_files,
                    .flags = &flags,
                });
            },
            else => {
                lib.addCSourceFiles(.{
                    .files = &cpp_files,
                    .flags = &cpp_flags,
                });
            },
        }

        lib.linkLibCpp();
        b.installArtifact(lib);
    }
}
