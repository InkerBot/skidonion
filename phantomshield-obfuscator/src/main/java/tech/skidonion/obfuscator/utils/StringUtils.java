package tech.skidonion.obfuscator.utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StringUtils {
    private static final String[] EMPTY_STRING_ARRAY = {};


    /**
     * Check that the given {@code String} is neither {@code null} nor of length 0.
     * <p>Note: this method returns {@code true} for a {@code String} that
     * purely consists of whitespace.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not {@code null} and has length
     * @see #hasText(String)
     */
    public static boolean hasLength(String str) {
        return (str != null && !str.isEmpty());
    }


    /**
     * Check whether the given {@code String} contains actual <em>text</em>.
     * <p>More specifically, this method returns {@code true} if the
     * {@code String} is not {@code null}, its length is greater than 0,
     * and it contains at least one non-whitespace character.
     *
     * @param str the {@code String} to check (may be {@code null})
     * @return {@code true} if the {@code String} is not {@code null}, its
     * length is greater than 0, and it does not contain whitespace only
     * @see #hasLength(String)
     * @see Character#isWhitespace
     */
    public static boolean hasText(String str) {
        return (str != null && !str.isEmpty() && containsText(str));
    }

    private static boolean containsText(CharSequence str) {
        int strLen = str.length();
        for (int i = 0; i < strLen; i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Replace all occurrences of a substring within a string with another string.
     *
     * @param inString   {@code String} to examine
     * @param oldPattern {@code String} to replace
     * @param newPattern {@code String} to insert
     * @return a {@code String} with the replacements
     */
    public static String replace(String inString, String oldPattern, String newPattern) {
        if (!hasLength(inString) || !hasLength(oldPattern) || newPattern == null) {
            return inString;
        }
        int index = inString.indexOf(oldPattern);
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString;
        }

        int capacity = inString.length();
        if (newPattern.length() > oldPattern.length()) {
            capacity += 16;
        }
        StringBuilder sb = new StringBuilder(capacity);

        int pos = 0;  // our position in the old string
        int patLen = oldPattern.length();
        while (index >= 0) {
            sb.append(inString, pos, index);
            sb.append(newPattern);
            pos = index + patLen;
            index = inString.indexOf(oldPattern, pos);
        }

        // append any characters to the right of a match
        sb.append(inString, pos, inString.length());
        return sb.toString();
    }


    public static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    /**
     * Tokenize the given {@code String} into a {@code String} array via a
     * {@link StringTokenizer}.
     * <p>The given {@code delimiters} string can consist of any number of
     * delimiter characters. Each of those characters can be used to separate
     * tokens. A delimiter is always a single character; for multi-character
     * delimiters, consider using @link #delimitedListToStringArray.
     *
     * @param str               the {@code String} to tokenize (potentially {@code null} or empty)
     * @param delimiters        the delimiter characters, assembled as a {@code String}
     *                          (each of the characters is individually considered as a delimiter)
     * @param trimTokens        trim the tokens via {@link String#trim()}
     * @param ignoreEmptyTokens omit empty tokens from the result array
     *                          (only applies to tokens that are empty after trimming; StringTokenizer
     *                          will not consider subsequent delimiters as token in the first place).
     * @return an array of the tokens
     * @see StringTokenizer
     * @see String#trim()
     */
    public static String[] tokenizeToStringArray(
            String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {

        if (str == null) {
            return EMPTY_STRING_ARRAY;
        }

        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens) {
                token = token.trim();
            }
            if (!ignoreEmptyTokens || token.length() > 0) {
                tokens.add(token);
            }
        }
        return tokens.toArray(new String[0]);
    }

    public static String convertClassNameToPath(String className) {
        return className.replace('.', '/');
    }

    public static String toDescriptor(String className) {
        StringBuilder sb = new StringBuilder();
        while (className.endsWith("[]")) {
            className = className.substring(0, className.length() - 2);
            sb.append("[");
        }
        switch (className) {
            case "*":
            case "**":
                return "**";
            case "void":
                sb.append("V");
                break;
            case "boolean":
                sb.append("Z");
                break;
            case "byte":
                sb.append("B");
                break;
            case "char":
                sb.append("C");
                break;
            case "short":
                sb.append("S");
                break;
            case "int":
                sb.append("I");
                break;
            case "float":
                sb.append("F");
                break;
            case "long":
                sb.append("J");
                break;
            case "double":
                sb.append("D");
                break;
            default:
                sb.append("L").append(convertClassNameToPath(className)).append(";");
                break;
        }
        return sb.toString();
    }


    public static String dynamicFormat(String string, Map<String, String> tokens) {
        String patternString = String.format("\\$(%s)",
                tokens.keySet().stream().map(StringUtils::unicodify).collect(Collectors.joining("|")));
        return replaceTokens(string, tokens, patternString);
    }

    public static String dynamicRawFormat(String string, Map<String, String> tokens) {
        if (tokens.isEmpty()) {
            return string;
        }

        return replaceTokens(string, tokens, "(" +
                tokens.keySet().stream().map(StringUtils::unicodify).collect(Collectors.joining("|")) + ")");
    }

    private static String replaceTokens(String string, Map<String, String> tokens, String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(string);

        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement(tokens.get(matcher.group(1))));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    public static String unicodify(String string) {
        StringBuilder result = new StringBuilder();
        for (char c : string.toCharArray()) {
            String hex = Integer.toHexString(c & 0xFFFF);
            result.append("\\u");
            for (int j = 0; j < 4 - hex.length(); j++) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

    public static String escapeCppNameString(String value) {
        Matcher m = Pattern.compile("([^a-zA-Z_0-9])").matcher(value);
        StringBuffer sb = new StringBuffer(value.length());
        while (m.find()) {
            m.appendReplacement(sb, String.format("u%d", (int) m.group(1).charAt(0)));
        }
        m.appendTail(sb);
        String output = sb.toString();
        if (output.length() > 0 && (output.charAt(0) >= '0' && output.charAt(0) <= '9')) {
            output = "_" + output;
        }
        return output;
    }

    public static String escapeCommentString(String value) {
        StringBuilder result = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= 32 && c <= 126) {
                result.append(c);
                continue;
            }
            String hex = Integer.toHexString(c & 0xFFFF);
            result.append("\\u");
            for (int j = 0; j < 4 - hex.length(); j++) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

    public static Map<String, String> createStringMap(Object... parts) {
        HashMap<String, String> tokens = new HashMap<>();
        for (int i = 0; i < parts.length; i += 2) {
            tokens.put(parts[i].toString(), parts[i + 1].toString());
        }
        return tokens;
    }

    public static Map<String, Object> createMap(Object... parts) {
        HashMap<String, Object> tokens = new HashMap<>();
        for (int i = 0; i < parts.length; i += 2) {
            tokens.put(parts[i].toString(), parts[i + 1]);
        }
        return tokens;
    }

}
