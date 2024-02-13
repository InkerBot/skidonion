import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.config.Config;
import tech.skidonion.obfuscator.config.ConfigBuilder;

import java.io.File;
import java.io.IOException;

public class ConfigTest {
    @Test
    void testConfig() throws IOException {
        ConfigBuilder configBuilder = new ConfigBuilder()
                .setInputJar(new File("input.jar"))
                .setOutputJar(new File("output"));
        Config config = configBuilder.build();
        config.save(new File("config.yaml"));
        assert config.getString("input").equals(new File("input.jar").getAbsoluteFile().toString());
        assert config.getString("output").equals(new File("output").getAbsoluteFile().toString());
    }
}
