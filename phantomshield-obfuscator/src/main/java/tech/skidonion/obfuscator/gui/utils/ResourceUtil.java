package tech.skidonion.obfuscator.gui.utils;

import tech.skidonion.obfuscator.utils.IOUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ResourceUtil {

    private ResourceUtil() {
    }

    public static ByteBuffer resourceToByteBuffer(String path, Class<?> clazz) {
        path = path.trim();
        final byte[] bytes = IOUtils.toByteArray(clazz.getResourceAsStream(path));
        final ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder()).put(bytes);
        data.flip();
        return data;
    }
}
