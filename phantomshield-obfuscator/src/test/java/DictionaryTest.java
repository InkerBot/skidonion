import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.DictionaryFactory;

import java.util.ArrayList;
import java.util.List;

public class DictionaryTest {
    private List<String> cached;

    @BeforeEach
    void setUp() {
        cached = new ArrayList<>();
    }

    @Test
    void testGenerated() {
        Dictionary dictionary = DictionaryFactory.get("spaces");
        for (int index = 0; index < 10000; index++) {
            String generated = dictionary.nextUniqueString();
            if (cached.contains(generated)) {
                throw new RuntimeException("unique?");
            } else {
                cached.add(generated);
                System.out.println(generated);
            }
        }
    }
}
