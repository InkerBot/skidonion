package tech.skidonion.obfuscator.value.impls;

import lombok.val;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RangeValueTest {

    @Test
    void name() {
        val value = new RangeValue("", 1, 1);
        value.parseConfig(new ArrayList<Number>() {
            {
                add(1);
                add(100);
            }
        });
        System.out.println(value.getRandomValue());
    }
}