package tech.skidonion.obfuscator.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {

    @Test
    void requestSoftwareInfo() {
        JsonObject jsonObject = VerifyUtils.requestSoftwareInformation(
                "http://localhost:8694/",
                "7",
                "fc5c8bf3750cf741378a0c672532583c",
                "1");

        System.out.println(jsonObject.toString());

    }
}