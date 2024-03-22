package tech.skidonion.obfuscator.value.impls;

import tech.skidonion.obfuscator.utils.RandomUtils;

public class ClassPackageValue extends StringValue {
    public ClassPackageValue(String name, String defaultValue) {
        super(name, defaultValue);
    }

    @Override
    public String getValue() {
        String origin = super.getValue();
        if (origin.isEmpty()) return origin;
        String path = origin.replace(".", "/");
        StringBuilder sb = new StringBuilder(path);
        if (!path.endsWith("/")) sb.append('/');
        for (int index = 0; (index = sb.indexOf("?", index)) != -1; ) {
            sb.replace(index, index + 1, RandomUtils.getRandomLetters(1));
        }
        return sb.toString();
    }
}
