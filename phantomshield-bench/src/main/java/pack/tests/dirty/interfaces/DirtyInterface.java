package pack.tests.dirty.interfaces;

public interface DirtyInterface {
    int count = 10;

    static String callableInterface() {
        return "PASS";
    }
}
