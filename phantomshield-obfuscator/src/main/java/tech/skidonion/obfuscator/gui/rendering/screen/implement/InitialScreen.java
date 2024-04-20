package tech.skidonion.obfuscator.gui.rendering.screen.implement;

import org.lwjgl.nanovg.NVGColor;
import org.lwjgl.nanovg.NVGPaint;
import tech.skidonion.obfuscator.gui.glfw.Window;
import tech.skidonion.obfuscator.gui.rendering.font.manager.FontManager;
import tech.skidonion.obfuscator.gui.rendering.objective2d.implement.Rectangle;
import tech.skidonion.obfuscator.gui.rendering.objective2d.implement.Text;
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

        final int width = Window.INSTANCE.getWindowWidth();
        final int height = Window.INSTANCE.getWindowHeight();

        nvgBeginPath(nvgContext);

        nvgRect(nvgContext, 0, 0, width, height);

        final NVGColor innerColor = NvgColorUtil.color(nvgContext, new Color(220, 170, 255));
        final NVGColor outerColor = NvgColorUtil.color(nvgContext, new Color(255, 255, 255));

        final NVGPaint paint = NVGPaint.calloc();
        nvgFillPaint(nvgContext, nvgRadialGradient(nvgContext, width / 2F, height * .95F, height / 10F, height, innerColor, outerColor, paint));

        innerColor.free();
        outerColor.free();
        paint.free();

        nvgFill(nvgContext);

        new Text(50, 50, "欢迎回来, 用户名", FontManager.NOTO_SANS_SC_MEDIUM, 40, NVG_ALIGN_LEFT | NVG_ALIGN_TOP, Color.BLACK).draw(nvgContext);
        new Text(50, 100, "距离您的订阅到期还剩：dd", FontManager.NOTO_SANS_SC_MEDIUM, 15, NVG_ALIGN_LEFT | NVG_ALIGN_TOP, Color.BLACK).draw(nvgContext);
    }
}
