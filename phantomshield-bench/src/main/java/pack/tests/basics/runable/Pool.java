package pack.tests.basics.runable;

import tech.skidonion.obfuscator.annotations.NativeObfuscation;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Pool {
    @NativeObfuscation.Inline
    public static ThreadPoolExecutor tpe = new ThreadPoolExecutor(0, 1, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1));
}
