package tech.skidonion.obfuscator.dictionary;

/**
 * String generation interface.
 */
public interface Dictionary {
    /**
     * @param length the length the generated string should be.
     * @return generates string randomly.
     */
    String randomString(int length);

    /**
     * @param length the length the generated string should be.
     * @return generates unique string randomly.
     */
    String uniqueRandomString(int length);

    /**
     * @return next unique string.
     */
    String nextUniqueString();

    /**
     * @return name of dictionary.
     */
    String getDictionaryName();

    Dictionary copy();
}
