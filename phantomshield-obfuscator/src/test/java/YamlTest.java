import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class YamlTest {
    @Test
    void testArray() {
        String __ = "array: [111,222]";

        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(__);
        System.out.println(map.toString());


    }
}
