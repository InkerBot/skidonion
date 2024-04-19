package tech.skidonion.obfuscator.gui.utility;

import org.lwjgl.nanovg.NVGColor;

import java.awt.*;

public final class NvgColorUtil {

    public static NVGColor create(Color color) {
        final NVGColor nvgColor = NVGColor.calloc();
        nvgColor.r(color.getRed() / 255F);
        nvgColor.g(color.getGreen() / 255F);
        nvgColor.b(color.getBlue() / 255F);
        nvgColor.a(color.getAlpha() / 255F);
        return nvgColor;
    }
}
