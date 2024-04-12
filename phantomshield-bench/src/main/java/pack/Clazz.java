package pack;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;


public class Clazz implements AutoCloseable {
    @NativeObfuscation.Inline
    public String test;
    @NativeObfuscation.Inline
    public static int a = 0;

    @NativeObfuscation.Inline
    public boolean test2;

    public static void main(String[] args) {
        print(allocate());
        print(allocate());
        print(allocate());
        print(allocate());
        print(allocate());
        print(allocate());
        print(allocate());
        Clazz gc = allocate();
        print(gc);
        gc.close();
        System.out.println("-----");
        // may crash
        print(gc);


        gc = allocate();
        print(gc);
        gc.close();
        System.out.println("-----");
        // again?
        print(gc);
    }

    public static Clazz allocate() {
        Clazz clazz = new Clazz();
        clazz.test = String.valueOf(a++);
        clazz.test2 = a % 2 == 1;
        return clazz;
    }

    public static void print(Clazz clazz) {
        System.out.println(clazz.test);
        System.out.println(clazz.test2);
    }

    @Override
    public void close() {

    }
}
