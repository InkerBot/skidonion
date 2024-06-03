package pack.tests.dirty;

public class Called implements CalledFace {
    private static String ret = "FAIL";

    static {
        ret = "PASS";
    }

    public void doPrint(Class<?> clazz) {
        int cnHash = clazz.getName().hashCode();
        if (Called.class.getName().hashCode() != cnHash) {
            ret = "FAIL";
        }
        if (CinitCall.class.getName().hashCode() == CinitCall.getIpp()
        ) {
            // Dumb : just let the javac don't optimize it
            ret = "FAIL";
        }
        System.out.println(ret);
    }
}
