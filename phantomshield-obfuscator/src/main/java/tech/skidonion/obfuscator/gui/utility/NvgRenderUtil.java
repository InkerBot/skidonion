package tech.skidonion.obfuscator.gui.utility;

import static org.lwjgl.nanovg.NanoVG.*;
import static tech.skidonion.obfuscator.gui.window.Window.*;

public final class NvgRenderUtil {

    public static void setup(Task task, long nvgContext) {
        switch (task) {
            case GRAPHICS_2D: {
                nvgBeginFrame(nvgContext, getWindowWidth(), getWindowHeight(), 1F);
                break;
            }
        }
    }

    public static void end(Task task, long nvgContext) {
        switch (task) {
            case GRAPHICS_2D: {
                nvgEndFrame(nvgContext);
                break;
            }
        }
    }

    // MAY ADD MORE IN FUTURE
    public enum Task {
        GRAPHICS_2D
    }
}
