package tech.skidonion.verification.crypto;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EdDSAPublicKeyTest {

    @Test
    void name() throws InvalidKeySpecException {
        EdDSAPublicKey pk = new EdDSAPublicKey(Base64.getDecoder().decode("MCowBQYDK2VwAyEAfZU0fSt8t0DWwlXSX4hF/TKN7NW+Z9CYy8/m3/Q5AAs="));
        System.out.println(Arrays.toString(pk.getAbyte()));

        System.out.println(Base64.getDecoder().decode("lw8mpkDRcC1KYH33VUIC0UvjHZR2+INL+MRwISye3rjKvUQu5cU4n0MZpbTKdg2NaT2aQCK40zeoha/kV4aUDQ==").length);

        System.out.println(Arrays.toString("涓栫晫绾ф崋缁戝寘".getBytes(StandardCharsets.UTF_8)));
    }
}