package tech.skidonion.verification.utils;

import jdk.nashorn.internal.scripts.JS;
import org.junit.jupiter.api.Test;

import java.io.Console;

import static org.junit.jupiter.api.Assertions.*;

class MachineIDUtilsTest {
    @Test
    void generate() {
        String[] a = new String[1];
        MachineIDUtils.generate(a);
        System.out.println(a[0]);
    }
}
