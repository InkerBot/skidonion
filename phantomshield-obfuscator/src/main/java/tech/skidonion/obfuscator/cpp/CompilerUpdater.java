package tech.skidonion.obfuscator.cpp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import tech.skidonion.obfuscator.downloader.DownloadBuilder;
import tech.skidonion.obfuscator.downloader.DownloadResult;
import tech.skidonion.obfuscator.utils.FileUtils;
import tech.skidonion.obfuscator.utils.HttpUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static tech.skidonion.obfuscator.PhantomShield.ERROR;
import static tech.skidonion.obfuscator.PhantomShield.INFO;

public class CompilerUpdater {
    public static final String DEFAULT_COMPILER = "bin/compiler/zig.exe";
    public static final String VERSION = "bin/compiler_version";

    public static void updateCompiler() {
        INFO("-----------------------");
        INFO("checking compiler for updates...");
        File versionFile = new File(VERSION);
        String version = null;
        if (versionFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(VERSION));) {
                version = reader.readLine();
            } catch (IOException e) {
                ERROR("error reading compiler version", e);
            }
        }
        INFO("requesting compiler version index...");
        JsonObject json = (JsonObject) JsonParser.parseString(HttpUtils.get("https://ziglang.org/download/index.json", null));

        JsonObject latest = json.get("master").getAsJsonObject();
        if (version != null) INFO("current version is " + version);
        String latestVersion = latest.get("version").getAsString();
        INFO("latest version is " + latestVersion);
        if (latestVersion.equals(version)) {
            INFO("compiler is up to date...");
        } else {
            JsonObject windows = latest.get("x86_64-windows").getAsJsonObject();
            FileUtils.clearDirectory(Paths.get("bin/compiler"));
            try {
                File temp = Files.createTempFile("zig", ".zip").toFile();

                DownloadBuilder builder = new DownloadBuilder()
                        .setUrl(windows.get("tarball").getAsString())
                        .setOutput(temp)
                        .setCallback(progress -> {
                            INFO(String.format("download progress: %.0f%%", progress * 100.0f));
                        })
                        .setOnFailure(() -> {
                            ERROR("error downloading compiler");
                        })
                        .setOnSuccess(() -> {
                            INFO("download complete");
                            decompressZig(temp.toPath(), Paths.get("bin/compiler"));
                        });
                Future<DownloadResult> future = builder.start();
                future.get();
                temp.delete();
                try (FileWriter writer = new FileWriter(versionFile)) {
                    writer.write(latestVersion);
                } catch (IOException e) {
                    ERROR("error writing compiler version", e);
                }
            } catch (IOException | ExecutionException | InterruptedException e) {
                ERROR("error downloading compiler", e);
            }
        }
        INFO("-----------------------");
    }

    private static void decompressZig(Path input, Path dir) {
        INFO("decompressing...");
        try (ZipFile zip = new ZipFile(input.toFile());) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();
                Path path = dir.resolve(entryName.substring(entryName.indexOf('/') + 1));
                try {
                    if (entry.isDirectory()) {
                        Files.createDirectories(path);
                    } else {
                        Files.createDirectories(path.getParent());
                        Files.copy(zip.getInputStream(entry), path);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        } catch (IOException e) {
            ERROR("error decompressing compiler", e);
        }
        INFO("decompression complete");
    }

}
