package pack.tests.dirty.clinit;

public class ClinitCall {
    static {
        new Called().doPrint(Called.class);
    }

    public static int i = 1;

    public static boolean called = false;

    public static void callMeInlined() {
        called = !called;
    }

    public static int getIpp() {
        return i++;
    }
}
