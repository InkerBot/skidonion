package tech.skidonion.obfuscator.inline;

import java.util.Objects;

public class Inline {
    /**
     * @param objects index0 will be given result the result value
     *                the last element is the value that will be checked
     *                the second last element is the expected value
     */
    public static void _verification_checkHardwareID(Object[] objects) {
        if (Objects.requireNonNull(objects).length > 0) {
            objects[0] = 0xFFFF_FFFF_FFFF_FFFFL;
        }
    }

    /**
     * @param objects the last element will be given hardware id
     */
    public static void _verification_generateHardwareID(Object[] objects) {
        if (Objects.requireNonNull(objects).length > 0) {
            objects[objects.length - 1] = "hardware_id";
        }
    }

    public static void trycatch() {

    }

    public static int _advanced_checkProtection(int expected) {
        return expected;
    }

    public static int _advanced_checkCRCImage(int expected) {
        return expected;
    }

    public static int _advanced_checkIsVirtualPC(int expected) {
        return expected;
    }

    public static int _advanced_checkIsDebuggerPresent(int expected) {
        return expected;
    }
}
