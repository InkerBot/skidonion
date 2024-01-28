import java.util.ArrayList;
import java.util.List;

public class ListTest {
    public static void main(String[] args) {
        List<String> a = new ArrayList<>();
        a.add("a");
        a.add("b");
        List<String> b = new ArrayList<>();
        b.add("b");
        b.add("a");
        System.out.println(a.equals(b));
    }
}
