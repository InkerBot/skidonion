package tech.skidonion.obfuscator.dictionary.impl;

import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.dictionary.StringSequence;
import tech.skidonion.obfuscator.utils.RandomUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates strings based on custom user-defined dictionary.
 */
public class CustomDictionary implements Dictionary {
    private final StringSequence CHARSET;
    private final Set<String> cache = new HashSet<>();
    private int index;

    public CustomDictionary(String charset) {
        this(new StringSequence(charset.toCharArray()));
    }

    public CustomDictionary(List<String> charset) {
        this(new StringSequence(charset));
    }

    public CustomDictionary(StringSequence strSequence) {
        CHARSET = strSequence;
    }

    @Override
    public String randomString(int length) {
        String[] c = new String[length];

        for (int i = 0; i < length; i++)
            c[i] = CHARSET.strAt(RandomUtils.getRandomInt(CHARSET.length()));

        return String.join("", c);
    }

    @Override
    public String uniqueRandomString(int length) {
        int count = 0;
        int arrLen = CHARSET.length();
        String s;

        do {
            s = randomString(length);

            if (count++ >= arrLen) {
                length++;
                count = 0;
            }
        } while (cache.contains(s));

        cache.add(s);
        return s;
    }

    @Override
    public String nextUniqueString() {

        String out = intToStr(index, CHARSET);
        if (cache.contains(out))
            throw new IllegalStateException("Cache contained string " + out);

        cache.add(out);
        index++;
        return out;
    }

    /**
     * @param index   A unique positive integer
     * @param charset A dictionary to permutate through
     * @return A unique string from for the given integer using permutations of the given charset
     */
    private String intToStr(int index, final StringSequence charset) {
        String[] buf = new String[100];
        int charPos = 99;

        index = -index; // Negate

        while (index <= -charset.length()) {
            buf[charPos--] = charset.strAt(-(index % charset.length()));
            index = index / charset.length();
        }
        buf[charPos] = charset.strAt(-index);

        String[] out = new String[100 - charPos];
        System.arraycopy(buf, charPos, out, 0, (100 - charPos));
        return String.join("", out);
    }

    @Override
    public String getDictionaryName() {
        return CHARSET.toString();
    }

    @Override
    public Dictionary copy() {
        return new CustomDictionary(CHARSET);
    }
}
