import tech.skidonion.obfuscator.inline.Wrapper;

public class Bootstrap {
    public static void main(String[] args) throws Exception {
        Wrapper._debug_addDefaultCloudConstant("授权验证用户组", "1984756007");
        Wrapper._debug_addDefaultCloudConstant("基础用户组", "108325887");
        Class.forName("tech.skidonion.obfuscator.cli.Main").getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
