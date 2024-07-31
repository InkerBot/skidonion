package pack.tests.dirty.ret;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

@NativeObfuscation
public class AllTypeReturn {
  public static void run() {
    if (runBoolean()
        && runByte() == 1
        && runChar() == 'a'
        && runShort() == 1
        && runInt() == 1
        && runLong() == 1
        && runFloat() == 1.0f
        && runDouble() == 1.0) {
      System.out.println("PASS");
    } else {
      System.out.println("FAIL");
    }
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static boolean runBoolean() {
    return true;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static byte runByte() {
    return 1;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static char runChar() {
    return 'a';
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static short runShort() {
    return 1;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static int runInt() {
    return 1;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static long runLong() {
    return 1;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static float runFloat() {
    return 1.0f;
  }

  @NativeObfuscation.Inline
  @NativeObfuscation
  public static double runDouble() {
    return 1.0;
  }
}
