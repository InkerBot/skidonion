import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.cpp.CompilerUpdater;

public class DownloadTest {
    @Test
    void update() {
        CompilerUpdater.updateCompiler();
    }
}
