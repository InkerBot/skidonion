package tech.skidonion.obfuscator.cli;

import picocli.CommandLine;
import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.cpp.CompilerUpdater;
import tech.skidonion.obfuscator.utils.commons.UTF8Control;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import static tech.skidonion.obfuscator.PhantomShield.*;

public class Main {
    @CommandLine.Command(name = "Phantom-Shield-X", mixinStandardHelpOptions = true, version = PhantomShield.VERSION, description = "Heavy Duty to Protect Your Jar")
    public static class CommandParser implements Callable<Integer> {
        @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true, description = "show this help message and exit")
        private boolean showHelp = false;
        @CommandLine.Option(names = {"-v", "--version"}, description = "print version information and exit")
        boolean showVersion = false;
        @CommandLine.Option(names = {"-u", "--update-compiler"}, description = "update jni library compiler")
        boolean update = false;
        @CommandLine.Option(paramLabel = "<config file>", names = {"-c", "--config"}, description = "input config file")
        File config;

        @Override
        public Integer call() throws Exception {
            if (update) {
                CompilerUpdater.updateCompiler();
                return 0;
            } else if (showVersion) {
                INFO("Phantom Shield X {}\n{}\n{}", PhantomShield.VERSION, "Copyright 2019-2024 fl0wowp4rty", "All rights reserved");
                return 0;
            } else if (config != null) {
                new PhantomShield(Config.readConfig(config)).process();
                return 0;
            }
            ERROR(TRANSLATION("phantom-shield-x.main.invalid"));
            ERROR(TRANSLATION("phantom-shield-x.main.try"));
            return -1;
        }
    }

    public static void main(String[] args) throws IOException {
        if (BUNDLE == null) {
            BUNDLE = ResourceBundle.getBundle("i18n.lang", new UTF8Control());
        }
        System.exit(new CommandLine(new CommandParser()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }
}
