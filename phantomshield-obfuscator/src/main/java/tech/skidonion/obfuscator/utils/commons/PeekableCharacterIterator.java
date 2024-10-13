package tech.skidonion.obfuscator.utils.commons;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PeekableCharacterIterator implements Iterator<Character> {
    private final String str;
    private int currentIndex = 0;
    private Character peekedChar = null;

    public PeekableCharacterIterator(String str) {
        this.str = str;
    }

    public Character peek() {
        if (peekedChar == null) {
            if (currentIndex < str.length()) {
                peekedChar = str.charAt(currentIndex);
            } else {
                return null;
            }
        }
        return peekedChar;
    }

    @Override
    public boolean hasNext() {
        return peek() != null;
    }

    @Override
    public Character next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Character result = peek();
        peekedChar = null;
        currentIndex++;
        return result;
    }
}
