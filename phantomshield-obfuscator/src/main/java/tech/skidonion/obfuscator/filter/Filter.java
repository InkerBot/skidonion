package tech.skidonion.obfuscator.filter;

import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;

import java.util.LinkedList;

public class Filter {
    private final Filter parent;
    private final LinkedList<Pattern> patterns = new LinkedList<>();

    public Filter() {
        this.parent = null;
    }

    public Filter(Filter parent) {
        this.parent = parent;
    }

    public void accept(String expression) {
        patterns.add(new Pattern(expression));
    }

    public void accept(String... expressions) {
        for (String expression : expressions) {
            patterns.add(new Pattern(expression));
        }
    }


    /**
     * @return return true then the class/method/field will be obfuscated
     */
    public boolean match(String expression) {
        boolean flag = !patterns.getFirst().isInclude();
        for (Pattern pattern : patterns) {
            if (flag == pattern.isInclude()) continue;
            if (pattern.match(expression, flag)) {
                flag = pattern.isInclude();
            }
        }
        return (parent == null || parent.match(expression)) && flag;
    }

    public boolean match(MethodWrapper method) {
        boolean flag = !patterns.getFirst().isInclude();
        for (Pattern pattern : patterns) {
            if (flag == pattern.isInclude()) continue;
            if (pattern.match(method, flag)) {
                flag = pattern.isInclude();
            }
        }
        return (parent == null || parent.match(method)) && flag;
    }

    public boolean match(FieldWrapper field) {
        boolean flag = !patterns.getFirst().isInclude();
        for (Pattern pattern : patterns) {
            if (flag == pattern.isInclude()) continue;
            if (pattern.match(field, flag)) {
                flag = pattern.isInclude();
            }
        }
        return (parent == null || parent.match(field)) && flag;
    }

    public boolean match(ClassWrapper clazz) {
        boolean flag = !patterns.getFirst().isInclude();
        for (Pattern pattern : patterns) {
            if (flag == pattern.isInclude()) continue;
            if (pattern.match(clazz, flag)) {
                flag = pattern.isInclude();
            }
        }
        return (parent == null || parent.match(clazz)) && flag;
    }

}
