import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.DictionaryFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GenerateUniqueMemberNameTest {

    private List<String> names;
    private Dictionary dictionary;

    @BeforeEach
    void setUp() {
        names = new ArrayList<>();
        for (int index = 0; index < 65535; index++) {
            names.add(Integer.toHexString(index));
        }
        dictionary = DictionaryFactory.get("spaces");
    }

    @Test
    void test() {
        for (int index = 0; index < 10000; index++) {
            generate();
        }
    }

    public String generate() {
        Set<String> methodNames = new HashSet<>(names);
        String generated;
        while (methodNames.contains(generated = dictionary.nextUniqueString())) ;
        return generated;
    }
}
