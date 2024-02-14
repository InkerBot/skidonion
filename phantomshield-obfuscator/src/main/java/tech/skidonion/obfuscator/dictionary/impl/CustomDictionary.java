package tech.skidonion.obfuscator.dictionary.impl;

import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.StringSequence;
import tech.skidonion.obfuscator.utils.RandomUtils;

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
    public String randomString(int length) {
        String[] c = new String[length];

        for (int i = 0; i < length; i++)
            c[i] = sequences.stringAt(RandomUtils.getRandomInt(sequences.size()));

        return String.join("", c);
    }


    @Override
    public String nextUniqueString() {
        int size = sequences.size();
        int i = uniqueIndex.getAndIncrement();
        String[] buf = new String[33];
        int position = 32;

        if ((i = -i) > 0) {
            throw new RuntimeException("Unique Index Can't be negative while generating dictionaries.");
        }

        while (i <= -size) {
            buf[position--] = sequences.stringAt(-(i % size));
            i /= size;
        }
        buf[position] = sequences.stringAt(-i);
        String[] result = new String[33 - position];

        System.arraycopy(buf, position, result, 0, result.length);
        return String.join("", result);
    }

    @Override
    public Dictionary copy() {
        return new CustomDictionary(sequences);
    }
}
