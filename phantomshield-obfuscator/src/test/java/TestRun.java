import tech.skidonion.obfuscator.PhantomShield;
import tech.skidonion.obfuscator.config.ConfigBuilder;

import java.io.File;

public class TestRun {
    public static void main(String[] args) {
        PhantomShield obfuscator = new PhantomShield(new ConfigBuilder()
                .setInputJar(new File("asm-9.6.jar"))
                .setOutputJar(new File("output\\asm-9.6.jar"))
                .setModeInvokeDynamicNativeConverter("enhancement")
                .build());
        obfuscator.process();
    }
}
