import tech.skidonion.obfuscator.trace.Tracer;

import java.io.File;
import java.nio.file.Paths;

public class TestTrace {
    public static void main(String[] args) {
        new Tracer(Paths.get("test", "Nekoium.jar").toFile(), false).trace().forEach(System.out::println);
    }
}
