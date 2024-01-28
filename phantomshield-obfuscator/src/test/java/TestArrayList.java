import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class TestArrayList {

    @Test
    public void test() {
        List<String> list = new ArrayList<>();
        list.add("test");
        list.add("apple");
        assert list.get(0).equals("test");
    }
}
