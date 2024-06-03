package pack.tests.dirty;

public class CinitCall {
    static {
        new Called().doPrint(Called.class);
    }

    public static int i = 1;

    public static int getIpp() {
        return i++;
    }
}
