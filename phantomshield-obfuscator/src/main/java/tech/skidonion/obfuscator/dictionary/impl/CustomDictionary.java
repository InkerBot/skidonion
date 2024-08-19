package tech.skidonion.obfuscator.dictionary.impl;

import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.StringSequence;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.Arrays;
import java.util.List;

/**
 * Generates strings based on custom user-defined dictionary.
 */
public class CustomDictionary extends Dictionary {
    private final StringSequence sequences;

    public CustomDictionary(String charset) {
        this(new StringSequence(charset.toCharArray()));
    }

    public CustomDictionary(List<String> charset) {
        this(new StringSequence(charset));
    }

    public CustomDictionary(StringSequence strSequence) {
        super(strSequence.toString());
        sequences = strSequence;
    }


    @Override
    public String next() {
        return generate(offset.get() + uniqueIndex.getAndIncrement());
    }

    @Override
    public int size() {
        return sequences.size();
    }

    @Override
    public String generate(long index) {
        int totalCharacterCount = size();

        int baseIndex = (int) (index / totalCharacterCount);
        int offset = (int) (index % totalCharacterCount);

        String newString = sequences.stringAt(offset);

        return baseIndex == 0 ? newString : (generate(baseIndex - 1) + newString);
    }

    @Override
    public Dictionary copy() {
        CustomDictionary copy = new CustomDictionary(sequences);
        copy.getOffset().set(this.getOffset().get());
        return copy;
    }
}
