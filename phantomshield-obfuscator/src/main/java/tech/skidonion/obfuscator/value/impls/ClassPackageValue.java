package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.utils.RandomUtils;

public class ClassPackageValue extends StringValue {
    private String cached = null;

    public ClassPackageValue(String name, String defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public String getValue() {
        if (cached != null)
            return cached;
        String origin = super.getValue();
        if (origin.isEmpty()) return cached == null ? cached = origin : cached;
        String path = origin.replace(".", "/");
        StringBuilder sb = new StringBuilder(path);
        if (!path.endsWith("/")) sb.append('/');
        for (int index = 0; (index = sb.indexOf("?", index)) != -1; ) {
            sb.replace(index, index + 1, RandomUtils.getRandomLetters(1));
        }
        return cached == null ? cached = sb.toString() : cached;
    }
}
