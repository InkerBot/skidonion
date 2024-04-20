package tech.skidonion.obfuscator.gui.rendering.screen.implement;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import tech.skidonion.obfuscator.gui.glfw.Window;
import tech.skidonion.obfuscator.gui.rendering.objective2d.implement.Rectangle;
import tech.skidonion.obfuscator.gui.rendering.screen.Screen;
import tech.skidonion.obfuscator.gui.utils.NvgColorUtil;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.nanovg.NanoVG.nvgFill;

public class InitialScreen extends Screen {

    @Override
    public void onInit() {
    }

    @Override
    public void draw(long nvgContext) {

    }

    private void drawBackground(long nvgContext) {
        final int width = Window.INSTANCE.getWindowWidth();
        final int height = Window.INSTANCE.getWindowHeight();

        nvgBeginPath(nvgContext);

        nvgRect(nvgContext, 0, 0, width, height);

        final NVGColor innerColor = NvgColorUtil.color(nvgContext, new Color(220, 170, 255));
        final NVGColor outerColor = NvgColorUtil.color(nvgContext, new Color(255, 255, 255));

        final NVGPaint paint = NVGPaint.calloc();
        nvgFillPaint(nvgContext, nvgRadialGradient(nvgContext, width / 2F, height / 1.5F, 0, height, innerColor, outerColor, paint));

        innerColor.free();
        outerColor.free();
        paint.free();

        nvgFill(nvgContext);
    }
}
