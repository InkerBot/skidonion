public class ClassPackage {
    public static void main(String[] args) {
        String clz = "java/lang/Object";
        System.out.println(clz.substring(0, clz.lastIndexOf('/') + 1));
    }
}
