package tech.skidonion.obfuscator.utils;

import com.google.gson.JsonIOException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {


    @Test
    void testEscapeCpp() {
        for (int i = 0; i < 100000; i++) {
            StringUtils.escapeCppNameString("java/lang/Object".replace("/", "_"));
        }
    }
}