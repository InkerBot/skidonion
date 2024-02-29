import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class MapTest {
    @Test
    void map() {
        Map<Integer, Integer> map = new HashMap<>();
        map.put(114514, 3);
        System.out.println(map.get(114514));
    }
}
