package tech.skidonion.obfuscator.utils.commons;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriorityObjectTest {
    @Test
    void test() {
        List<PriorityObject<String>> list = new ArrayList<>();
        list.add(new PriorityObject<>("3", 3));
        list.add(new PriorityObject<>("1", 1));
        list.add(new PriorityObject<>("2", 2));
        System.out.println(list);
        Collections.sort(list);
        System.out.println(list);
    }
}