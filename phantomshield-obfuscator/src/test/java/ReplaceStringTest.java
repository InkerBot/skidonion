import org.junit.jupiter.api.Test;

public class ReplaceStringTest {
    @Test
    void test() {
        long start = System.currentTimeMillis();
        String value = "skidonion/???/";
        for (int i = 0; i < 100_000_000; i++) {

            StringBuilder sb = new StringBuilder(value);
            if (value.endsWith("/")) sb.deleteCharAt(sb.length() - 1);
            for (int index = 0;
                 (index = sb.indexOf("?", index)) != -1;
                 sb.replace(index, index + 1, String.valueOf((char) (Math.random() * 26 + 'a')))) {
            }
        }

        System.out.println("time used: " + (System.currentTimeMillis() - start));
    }
}
