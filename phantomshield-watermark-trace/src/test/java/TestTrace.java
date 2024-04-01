import tech.skidonion.obfuscator.trace.Tracer;

import java.io.File;

public class TestTrace {
    public static void main(String[] args) {
        new Tracer(new File("D:\\runtimes\\java-obfuscator\\phantom-shield-x\\phantomshield-obfuscator.jar")).trace().forEach(System.out::println);
    }
}
