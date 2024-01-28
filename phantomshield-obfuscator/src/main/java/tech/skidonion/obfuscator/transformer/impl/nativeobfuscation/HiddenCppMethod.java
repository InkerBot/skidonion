package tech.skidonion.obfuscator.transformer.impl.nativeobfuscation;

public class HiddenCppMethod {

    private final HiddenMethodsPool.HiddenMethod hiddenMethod;

    private final String cppName;

    public HiddenCppMethod(HiddenMethodsPool.HiddenMethod hiddenMethod, String cppName) {
        this.hiddenMethod = hiddenMethod;
        this.cppName = cppName;
    }

    public HiddenMethodsPool.HiddenMethod getHiddenMethod() {
        return hiddenMethod;
    }

    public String getCppName() {
        return cppName;
    }
}
