package tech.skidonion.obfuscator.trace;

import java.io.File;
import java.util.Objects;

public class Main {
    public static void main(String[] args) {
        if (args.length > 1) {
            boolean useOldCrypto = args.length == 2 && Objects.equals(args[1], "-v1");
            new Tracer(new File(args[0]), useOldCrypto).trace().forEach(System.out::println);
        } else {
            System.out.println("Usage: java -jar phantomshield-watermark-trace.jar <input jar path> [-v1]\n\nCopyright 2024 fl0wowp4rty\nAll rights reserved");
        }
    }

}
