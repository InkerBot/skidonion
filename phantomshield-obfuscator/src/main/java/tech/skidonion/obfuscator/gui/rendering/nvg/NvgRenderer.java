package tech.skidonion.obfuscator.gui.rendering.nvg;

import org.lwjgl.nanovg.NanoVGGL2;
import org.lwjgl.nanovg.NanoVGGL3;
import org.lwjgl.opengl.GL30;
import tech.skidonion.obfuscator.gui.glfw.Window;
import tech.skidonion.obfuscator.gui.rendering.font.Font;
import tech.skidonion.obfuscator.gui.rendering.font.manager.FontManager;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class NvgRenderer {

    public static NvgRenderer INSTANCE = new NvgRenderer();

    private long nvgContext = NULL;

    public long getNvgContext() {
        return nvgContext;
    }

    private boolean usingGl3;

    public boolean isUsingGl3() {
        return usingGl3;
    }

    private final List<Font> registered = new ArrayList<>();

    private NvgRenderer() {
    }

    public boolean init() {
        usingGl3 = (glGetInteger(GL30.GL_MAJOR_VERSION) > 3) || (glGetInteger(GL30.GL_MAJOR_VERSION) == 3 && glGetInteger(GL30.GL_MINOR_VERSION) >= 2);
        if (usingGl3) {
            nvgContext = NanoVGGL3.nvgCreate(NanoVGGL3.NVG_ANTIALIAS | NanoVGGL3.NVG_STENCIL_STROKES);
        } else {
            nvgContext = NanoVGGL2.nvgCreate(NanoVGGL2.NVG_ANTIALIAS | NanoVGGL2.NVG_STENCIL_STROKES);
        }
        return nvgContext != NULL;
    }

    public void destroy() {
        if (usingGl3) {
            NanoVGGL3.nvgDelete(nvgContext);
        } else {
            NanoVGGL2.nvgDelete(nvgContext);
        }
    }

    public void setup() {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        final List<Font> registeredFont = FontManager.INSTANCE.getRegistered();

        for (Font font : registeredFont) {
            if (!registered.contains(font)) {
                nvgCreateFontMem(nvgContext, font.getName(), font.getByteBuffer(), false);
                registered.add(font);
            }
        }

        nvgBeginFrame(nvgContext, Window.INSTANCE.getWindowWidth(), Window.INSTANCE.getWindowHeight(), 1);
    }

    public void end() {
        nvgEndFrame(nvgContext);

        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
    }
}
