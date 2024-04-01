package tech.skidonion.obfuscator.utils;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VerifyUtilsTest {

    @Test
    void requestSoftwareInfo() {
        JsonObject jsonObject = VerifyUtils.requestSoftwareInformation(
                "https://skidonion.tech/",
                "1",
                "769e4f678db8436b0018fc6fe60a5a7a",
                "1");

        System.out.println(jsonObject.toString());

    }
}