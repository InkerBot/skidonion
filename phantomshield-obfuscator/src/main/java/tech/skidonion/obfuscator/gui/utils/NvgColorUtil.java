package tech.skidonion.obfuscator.gui.utils;

import org.lwjgl.nanovg.NVGColor;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.nvgFillColor;
import static org.lwjgl.nanovg.NanoVG.nvgRGBA;

public final class NvgColorUtil {

    public static NVGColor color(long nvgContext, Color color) {
        NVGColor nvgColor = NVGColor.calloc();
        nvgRGBA((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue(), (byte) color.getAlpha(), nvgColor);
        nvgFillColor(nvgContext, nvgColor);
        return nvgColor;
    }
}
