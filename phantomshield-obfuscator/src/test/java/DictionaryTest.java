import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.DictionaryFactory;

import java.util.HashSet;
import java.util.Set;

public class DictionaryTest {
    private Set<String> cached;

    @BeforeEach
    void setUp() {
        cached = new HashSet<>();
    }

    @Test
    void testGenerated() {
        Dictionary dictionary = DictionaryFactory.get("spaces");
        for (int index = 0; index < 100000; index++) {
            String generated = dictionary.nextUniqueString();
            if (!cached.add(generated)) {
                throw new RuntimeException("unique?");
            }
        }
    }
}
