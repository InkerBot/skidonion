import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

public class TestGenerateCloudConstant {
    @Test
    void generate() {
        System.out.println("基础用户组".hashCode());
        System.out.println("授权验证用户组".hashCode());

        int r1 = ThreadLocalRandom.current().nextInt();
        int r2 = ThreadLocalRandom.current().nextInt();
        int xor = r1 ^ r2;
        System.out.println(r1);
        System.out.println(r2);
        System.out.println(xor);
    }
}
