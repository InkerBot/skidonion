package tech.skidonion.obfuscator.trace;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        if (args.length == 1) {
            new Tracer(new File(args[0])).trace().forEach(System.out::println);
        } else {
            System.out.println("Usage: java -jar phantomshield-watermark-trace.jar <input jar path>\n\nCopyright 2024 fl0wowp4rty\nAll rights reserved");
        }
    }

}
