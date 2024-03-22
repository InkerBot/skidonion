package tech.skidonion.verification.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test
    void parse() {
        JsonValue parse = Json.parse("{\"abc\": \"123\"}");
        if (parse.isObject()) {
            assertEquals("123", parse.asObject().get("abc").asString());
        }
    }
}