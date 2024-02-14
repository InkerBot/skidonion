package tech.skidonion.obfuscator.dictionary.impl;

import java.util.ArrayList;
import java.util.List;

public class KeywordDictionary extends CustomDictionary {
    private static final List<String> KEYWORDS;

    static {
        KEYWORDS = new ArrayList<>();
        KEYWORDS.add("\u2000private");
        KEYWORDS.add("\u2000protected");
        KEYWORDS.add("\u2000public");
        KEYWORDS.add("\u2000abstract");
        KEYWORDS.add("\u2000class");
        KEYWORDS.add("\u2000extends");
        KEYWORDS.add("\u2000final");
        KEYWORDS.add("\u2000implements");
        KEYWORDS.add("\u2000interface");
        KEYWORDS.add("\u2000native");
        KEYWORDS.add("\u2000new");
        KEYWORDS.add("\u2000static");
        KEYWORDS.add("\u2000strictfp");
        KEYWORDS.add("\u2000synchronized");
        KEYWORDS.add("\u2000transient");
        KEYWORDS.add("\u2000volatile");
        KEYWORDS.add("\u2000break");
        KEYWORDS.add("\u2000case");
        KEYWORDS.add("\u2000continue");
        KEYWORDS.add("\u2000do");
        KEYWORDS.add("\u2000else");
        KEYWORDS.add("\u2000for");
        KEYWORDS.add("\u2000if");
        KEYWORDS.add("\u2000instanceof");
        KEYWORDS.add("\u2000return");
        KEYWORDS.add("\u2000switch");
        KEYWORDS.add("\u2000while");
        KEYWORDS.add("\u2000assert");
        KEYWORDS.add("\u2000case");
        KEYWORDS.add("\u2000finally");
        KEYWORDS.add("\u2000throw");
        KEYWORDS.add("\u2000throws");
        KEYWORDS.add("\u2000try");
        KEYWORDS.add("\u2000import");
        KEYWORDS.add("\u2000package");
        KEYWORDS.add("\u2000boolean");
        KEYWORDS.add("\u2000byte");
        KEYWORDS.add("\u2000char");
        KEYWORDS.add("\u2000double");
        KEYWORDS.add("\u2000float");
        KEYWORDS.add("\u2000int");
        KEYWORDS.add("\u2000long");
        KEYWORDS.add("\u2000short");
        KEYWORDS.add("\u2000super");
        KEYWORDS.add("\u2000this");
        KEYWORDS.add("\u2000void");
        KEYWORDS.add("\u2000goto");
        KEYWORDS.add("\u2000const");
    }

    public KeywordDictionary() {
        super(KEYWORDS);
        setName("keywords");
    }
}
