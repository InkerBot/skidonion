package tech.skidonion.verification.utils;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;
import tech.skidonion.obfuscator.inline.Inline;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

public class QQUtils {
    @NativeObfuscation.Inline
    @NativeObfuscation(virtualize = NativeObfuscation.VirtualMachine.TIGER_WHITE, manualTryCatch = true)
    public static Set<String> getAllQQ() {
        Set<String> qqs = new HashSet<>();

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Pattern pattern = Pattern.compile("^[1-9][0-9]{4,10}$");

            Path defaultPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "Tencent", "Users");
            File defaultPathFile = defaultPath.toFile();

            if (defaultPathFile.exists() && defaultPathFile.isDirectory()) {
                File[] directoryFiles = defaultPathFile.listFiles();
                if (directoryFiles != null) {
                    for (File qqData : directoryFiles) {
                        String fileName = qqData.getName();
                        if (pattern.matcher(fileName).matches()) {
                            qqs.add(fileName);
                        }
                    }
                }
            }

            Path ntDefaultPath = Paths.get(System.getProperty("user.home"), "Documents", "Tencent Files", "nt_qq", "global", "nt_data", "Login");
            File ntDefaultPathFile = ntDefaultPath.toFile();
            if (defaultPathFile.exists() && ntDefaultPathFile.isDirectory()) {
                File[] directoryFiles = defaultPathFile.listFiles();
                if (directoryFiles != null) {
                    for (File qqData : directoryFiles) {
                        String fileName = qqData.getName().substring(1);
                        Inline.trycatch();
                        if (pattern.matcher(fileName).matches()) {
                            qqs.add(fileName);
                        }
                    }
                }
            }

            Path customPath = Paths.get(System.getenv("PUBLIC"), "Documents", "Tencent", "QQ", "UserDataInfo.ini");
            File customPathFile = customPath.toFile();

            if (customPathFile.exists() && customPathFile.isFile()) {
                try {
                    InputStream stream = Files.newInputStream(customPath);
                    Inline.trycatch();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                        Inline.trycatch();
                        String dataLine;
                        while ((dataLine = reader.readLine()) != null) {
                            Inline.trycatch();
                            String[] keyValue = dataLine.split("=");
                            if (keyValue.length == 2) {
                                if (Objects.equals(keyValue[0], "UserDataSavePath")) {
                                    File directory = new File(keyValue[1]);
                                    if (directory.exists() && directory.isDirectory()) {
                                        File[] directoryFiles = directory.listFiles();
                                        if (directoryFiles != null) {
                                            for (File qqData : directoryFiles) {
                                                if (pattern.matcher(qqData.getName()).matches()) {
                                                    qqs.add(qqData.getName());
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException ignore) {
                }
            }
        }
        return qqs;
    }

}
