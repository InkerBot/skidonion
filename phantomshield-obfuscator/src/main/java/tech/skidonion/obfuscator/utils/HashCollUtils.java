package tech.skidonion.obfuscator.utils;

import java.io.PrintWriter;

public class HashCollUtils {
    public static final int CHAR_MAX = 'z';
    public static final int CHAR_MIN = '0';

    public static final char[] CHAR_TYPES = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '_', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

    private final char[] buffer = new char[128];
    private int bufferIndex = 0;

    private static final long[] pow31 = new long[16];
    private static final long[] pow31Sum = new long[16];

    static {
        pow31[0] = 1;
        pow31Sum[0] = 1;
        for (int i = 1; i < 16; i++) {
            pow31[i] = pow31[i - 1] * 31;
            pow31Sum[i] = pow31Sum[i - 1] + pow31[i];
        }
    }

    String callback = null;

    private void search(long code, int len) throws InterruptedException {
        if (len == 0) {
            if (code == 0) {
                callback = new String(buffer, 0, bufferIndex);
            }
            return;
        }

        long codeMax = CHAR_MAX * pow31Sum[len - 1];
        if (code > codeMax) {
            return;
        }

        long codeMin = CHAR_MIN * pow31Sum[len - 1];
        if (code < codeMin) {
            return;
        }

        for (char c : CHAR_TYPES) {
            long temp = code - c * pow31[len - 1];
            if (temp < 0) {
                break;
            }
            buffer[bufferIndex++] = c;
            search(temp, len - 1);
            bufferIndex--;
        }
    }

    private void searchWrapper(long code) {
        int len = 0;
        long temp = code;
        while (temp >= CHAR_MIN) {
            len++;
            temp /= 31;
        }
        try {
            search(code, len);
        } catch (InterruptedException e) {
        }
    }

    public String search(long code) {
        long inc = 0;
        while (true) {
            searchWrapper(code + (inc << 32));
            if (callback != null) {
                return callback;
            }
            inc++;
        }
    }

    public static void main(String[] args) throws Exception {
        PrintWriter pw = new PrintWriter(System.out);

        long code = "HsTeam".hashCode();
        System.out.println(code);

        HashCollUtils gen = new HashCollUtils();
        String result = gen.search(code);
        System.out.println(result);
        System.out.println(result.hashCode());
    }
}
