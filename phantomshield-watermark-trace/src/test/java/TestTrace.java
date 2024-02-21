import tech.skidonion.obfuscator.trace.Tracer;

import java.io.File;

public class TestTrace {
    public static void main(String[] args) {
        new Tracer(new File("test\\output\\obf-test-1.0-SNAPSHOT.jar")).trace().forEach(System.out::println);
    }
}
