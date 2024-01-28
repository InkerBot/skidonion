package tech.skidonion.obfuscator.filter;

import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.asm.FieldWrapper;
import tech.skidonion.obfuscator.asm.MethodWrapper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * [@annotations] class [implements/extends] [class] [@annotations] return_type method(arguments)
 * [@annotations] class [implements/extends] [class] [@annotations] return_type field
 * [@java.lang.annotation.Target *.* implements ]
 */
public class Pattern {
    private final static AntPathMatcher MATCHER = new AntPathMatcher();

    private final boolean include;
    private final FilterInformation filterInformation;

    public Pattern(String expression) {
        include = expression.startsWith("+");
        expression = expression.substring(1);
        filterInformation = FilterInformation.resolve(expression);
    }


    public boolean match(ClassWrapper clazz, boolean defaultResult) {
        if (filterInformation.getType() != FilterInformation.FilterType.CLASS) return defaultResult;
        return MATCHER.match(filterInformation.getOwner(), clazz.getOriginalName()) &&
                (filterInformation.getOwnerExtends() == null || clazz.getOriginalSuperName() == null || MATCHER.match(filterInformation.getOwnerExtends(), clazz.getOriginalSuperName())) &&
                matchAll(filterInformation.getOwnerAnnotations(), clazz.getOriginalAnnotations().stream().map(annotationNode -> annotationNode.desc).collect(Collectors.toList())) &&
                matchAll(filterInformation.getOwnerImplements(), clazz.getOriginalInterfaces());
    }

    public boolean match(MethodWrapper method, boolean defaultResult) {
        if (filterInformation.getType() != FilterInformation.FilterType.METHOD) return defaultResult;
        return MATCHER.match(filterInformation.getOwner(), method.getOwner().getOriginalName()) &&
                (filterInformation.getOwnerExtends() == null || method.getOwner().getOriginalSuperName() == null || MATCHER.match(filterInformation.getOwnerExtends(), method.getOwner().getOriginalSuperName())) &&
                matchAll(filterInformation.getOwnerAnnotations(), method.getOwner().getOriginalAnnotations().stream().map(annotationNode -> annotationNode.desc).collect(Collectors.toList())) &&
                matchAll(filterInformation.getOwnerImplements(), method.getOwner().getOriginalInterfaces()) &&
                MATCHER.match(filterInformation.getMember(), method.getOriginalName()) &&
                MATCHER.match(filterInformation.getDescriptor(), method.getOriginalDescription()) &&
                matchAll(filterInformation.getMemberAnnotations(), method.getOriginalAnnotations().stream().map(annotationNode -> annotationNode.desc).collect(Collectors.toList()));
    }

    public boolean match(FieldWrapper field, boolean defaultResult) {
        if (filterInformation.getType() != FilterInformation.FilterType.FIELD) return defaultResult;
        return MATCHER.match(filterInformation.getOwner(), field.getOwner().getOriginalName()) &&
                (filterInformation.getOwnerExtends() == null || field.getOwner().getOriginalSuperName() == null || MATCHER.match(filterInformation.getOwnerExtends(), field.getOwner().getOriginalSuperName())) &&
                matchAll(filterInformation.getOwnerAnnotations(), field.getOwner().getOriginalAnnotations().stream().map(annotationNode -> annotationNode.desc).collect(Collectors.toList())) &&
                matchAll(filterInformation.getOwnerImplements(), field.getOwner().getOriginalInterfaces()) &&
                MATCHER.match(filterInformation.getMember(), field.getOriginalName()) &&
                MATCHER.match(filterInformation.getDescriptor(), field.getOriginalDescription()) &&
                matchAll(filterInformation.getMemberAnnotations(), field.getOriginalAnnotations().stream().map(annotationNode -> annotationNode.desc).collect(Collectors.toList()));
    }

    public boolean match(String expression, boolean defaultResult) {
        FilterInformation info = FilterInformation.resolve(expression);
        if (info.getType() != filterInformation.getType()) return defaultResult;
        if (info.equals(filterInformation)) return true;
        if (info.getType() == FilterInformation.FilterType.CLASS) {
            return MATCHER.match(filterInformation.getOwner(), info.getOwner()) &&
                    (filterInformation.getOwnerExtends() == null || info.getOwnerExtends() == null || MATCHER.match(filterInformation.getOwnerExtends(), info.getOwnerExtends())) &&
                    matchAll(filterInformation.getOwnerAnnotations(), info.getOwnerAnnotations()) &&
                    matchAll(filterInformation.getOwnerImplements(), info.getOwnerImplements());
        } else if (info.getType() == FilterInformation.FilterType.METHOD || info.getType() == FilterInformation.FilterType.FIELD) {
            return MATCHER.match(filterInformation.getOwner(), info.getOwner()) &&
                    (filterInformation.getOwnerExtends() == null || info.getOwnerExtends() == null || MATCHER.match(filterInformation.getOwnerExtends(), info.getOwnerExtends())) &&
                    matchAll(filterInformation.getOwnerAnnotations(), info.getOwnerAnnotations()) &&
                    matchAll(filterInformation.getOwnerImplements(), info.getOwnerImplements()) &&
                    MATCHER.match(filterInformation.getMember(), info.getMember()) &&
                    MATCHER.match(filterInformation.getDescriptor(), info.getDescriptor()) &&
                    matchAll(filterInformation.getMemberAnnotations(), info.getMemberAnnotations());
        }
        throw new RuntimeException("unknown filter type: " + info.getType());
    }

    private boolean matchAll(List<String> patterns, List<String> matches) {
        if (patterns.isEmpty()) return true;
        if (matches.size() < patterns.size()) return false;
        Set<String> _matches = new HashSet<>(matches);
        each:
        for (String pattern : patterns) {
            for (Iterator<String> iterator = _matches.iterator(); iterator.hasNext(); ) {
                String match = iterator.next();
                if (MATCHER.match(pattern, match)) {
                    iterator.remove();
                    continue each;
                }
            }
            return false;
        }
        return true;
    }

    public boolean isInclude() {
        return include;
    }
}
