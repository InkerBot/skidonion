import tech.skidonion.obfuscator.inline.Inline;

public class Main {
    public static void main(String[] args) {
        Object[] objects = new Object[3];
        objects[1] = 2;
        Inline._verification_generateHardwareID(objects);
        System.out.println(objects[2]);
        Inline._verification_checkHardwareID(objects);
        System.out.println((((long) objects[0] >> 32) ^ (int) objects[1]) & 0b1);
        System.out.println(Inline._advanced_checkProtection(114514));
    }
}
