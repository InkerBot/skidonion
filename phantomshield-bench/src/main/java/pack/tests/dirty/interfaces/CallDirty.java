package pack.tests.dirty.interfaces;

public class CallDirty {
    public void doPrint() {
        int t = DirtyInterface.count;
        int t2 = DirtyClass.count;
        DirtyClass.count++;
        String s = DirtyInterface.callableInterface();
        String s2 = DirtyClass.callableInterface();
        if (s.equals(s2)) {
            if (t == t2) {
                if (DirtyClass.count == 12) {
                    System.out.println("PASS");
                    return;
                }
            }
        }
        System.out.println("FAIL");
    }
}
