package tech.skidonion.verification.crypto;

import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Base64Test {
    @Test
    void encode() throws UnsupportedEncodingException {

        System.out.println(new String(Base64.getDecoder().decode(tech.skidonion.verification.crypto.Base64.encode("123".getBytes(StandardCharsets.UTF_8)))));
    }
}