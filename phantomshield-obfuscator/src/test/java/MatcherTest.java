import org.junit.jupiter.api.Test;
import tech.skidonion.obfuscator.filter.Filter;

public class MatcherTest {
    @Test
    void testAll() {
        Filter filter = new Filter();
        filter.accept("+@java.lang.annotations.** **");
        System.out.println(filter.match("@java.lang.annotations.Documented java.lang.String"));
        System.out.println(filter.match("@java.lang.a.SB java.lang.String"));
        filter.accept("-**");
        System.out.println(filter.match("@java.lang.annotations.Documented java.lang.String"));
        System.out.println(filter.match("java.lang.String"));
    }

    @Test
    void matchMethodAndField() {
        Filter filter = new Filter();
        filter.accept("+@Test Test * Test");
        System.out.println(filter.match("@Test Test int Test")); // true
        System.out.println(filter.match("Test int Test")); // false
        System.out.println(filter.match("@Test Test boolean Test")); // true
        System.out.println(filter.match("@Test Test int Test()")); // false
        filter.accept("+Test int Test()");
        System.out.println(filter.match("Test int Test()")); // true
        System.out.println(filter.match("Test int Test(int)")); // false

//        true
//        false
//        true
//        false
//        true
//        false
    }
}
