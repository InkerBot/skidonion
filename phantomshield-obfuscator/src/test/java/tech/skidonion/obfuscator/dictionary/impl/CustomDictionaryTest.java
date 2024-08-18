package tech.skidonion.obfuscator.dictionary.impl;

import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.DictionaryFactory;

class CustomDictionaryTest {
    @Test
    void testGenerateName() {
        int length = 5;
        Dictionary dictionary = DictionaryFactory.get("ilI1");
        int n = 0;
        for (int i = 1; i < length ; i++) {
            n += (int) Math.pow(dictionary.size(), i);
        }
        dictionary.getOffset().set(n);
        for (int i = 0; i < 100; i++) {
            System.out.println(dictionary.next());
        }
    }
}