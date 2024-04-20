package tech.skidonion.obfuscator.gui.rendering.font;

import java.nio.ByteBuffer;

public final class Font {

    private final String name;

    public String getName() {
        return name;
    }

    private final ByteBuffer byteBuffer;

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public Font(String nameIn, ByteBuffer byteBufferIn) {
        name = nameIn;
        byteBuffer = byteBufferIn;
    }
}
