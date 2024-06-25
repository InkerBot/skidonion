package pack.tests.dirty.interfaces;

public abstract class DirtyClass {
    public static int count = 10;

    static String callableInterface() {
        count++;
        return "PASS";
    }

    public abstract void notImplemented();
}
