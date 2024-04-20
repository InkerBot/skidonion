package tech.skidonion.obfuscator.gui.rendering.font.manager;

import tech.skidonion.obfuscator.gui.rendering.font.Font;
import tech.skidonion.obfuscator.utils.IOUtils;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FontManager {

    public static FontManager INSTANCE = new FontManager();

    public static final String NOTO_SANS_SC_MEDIUM = "NotoSansSC-Medium";

    private final List<Font> registered = new ArrayList<>();

    public List<Font> getRegistered() {
        return registered;
    }

    private FontManager() {
        {
            final InputStream notoSansStream = Objects.requireNonNull(getClass().getResourceAsStream("/gui/font/NotoSansSC-Medium.ttf"));

            final byte[] bytes = IOUtils.toByteArray(notoSansStream);
            final ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder()).put(bytes);
            data.flip();

            registered.add(new Font(NOTO_SANS_SC_MEDIUM, data));
        }
    }
}
