package tech.skidonion.obfuscator.dictionary.impl;

import tech.skidonion.obfuscator.dictionary.Dictionary;
import tech.skidonion.obfuscator.utils.RandomUtils;

/**
 * Generates strings full of spaces.
 */
public class SpacesDictionary extends Dictionary {
    private static final char[] CHARSET = new char[0xF + 1];

    static {
        for (int i = 0; i < CHARSET.length; i++)
            CHARSET[i] = (char) ('\u2000' + i);
    }

    public SpacesDictionary() {
        super("spaces");
    }

    @Override
    public String randomString(int length) {
        char[] c = new char[length];

        for (int i = 0; i < length; i++)
            c[i] = CHARSET[RandomUtils.getRandomInt(CHARSET.length)];

        return new String(c);
    }


    @Override
    public String nextUniqueString() {
        int charsetLength = CHARSET.length;
        int i = uniqueIndex.getAndIncrement();
        char[] buf = new char[33];
        int charPos = 32;

        if ((i = -i) > 0) {
            throw new RuntimeException("Unique Index Can't be negative while generating dictionaries.");
        }

        while (i <= -charsetLength) {
            buf[charPos--] = CHARSET[-(i % charsetLength)];
            i /= charsetLength;
        }
        buf[charPos] = CHARSET[-i];

        return new String(buf, charPos, (33 - charPos));
    }


    @Override
    public Dictionary copy() {
        return new SpacesDictionary();
    }
}
