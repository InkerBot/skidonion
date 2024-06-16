package pack.tests.dirty.clinit;

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
        if (ClinitCall.class.getName().hashCode() == ClinitCall.getIpp()
        ) {
            // Dumb : just let the javac don't optimize it
            ret = "FAIL";
        }
        System.out.println(ret);
    }
}
