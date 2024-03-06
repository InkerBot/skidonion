import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeFormatterTest {
    @Test
    void test() {
        System.out.println(new SimpleDateFormat("yyyy-MM-dd-hhmmss").format(new Date()));
    }
}
