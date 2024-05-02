package pack;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

@NativeObfuscation
public class Clazz implements AutoCloseable {
    @NativeObfuscation.Inline
    public String test;
    @NativeObfuscation.Inline
    public static int a = 0;

    @NativeObfuscation.Inline
    public boolean test2;

    public static void main(String[] args) {
        System.out.println("method lock:");
        System.out.println(test());
        System.out.println("-----");
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

    @NativeObfuscation.Inline
    public static Clazz allocate() {
        Clazz clazz = new Clazz();
        clazz.test = String.valueOf(a++);
        clazz.test2 = a % 2 == 1;
        return clazz;
    }

    public static void print(Clazz clazz) {
        System.out.println(123);
        System.out.println(clazz.test);
        System.out.println(clazz.test2);
    }

    @NativeObfuscation(verificationLock = "基础用户组")
    public static String test() {
        return "测试";
    }

    @Override
    public void close() {

    }
}
