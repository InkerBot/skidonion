package tech.skidonion.obfuscator.gui.rendering.objective2d.implement;

import org.lwjgl.nanovg.NVGColor;
import tech.skidonion.obfuscator.gui.rendering.objective2d.Objective2D;
import tech.skidonion.obfuscator.gui.utils.NvgColorUtil;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class Rectangle extends Objective2D {

    private int width, height;

    public int getWidth() {
        return width;
    }

    public void setWidth(int widthIn) {
        width = widthIn;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int heightIn) {
        height = heightIn;
    }

    private Color color;

    public Color getColor() {
        return color;
    }

    public void setColor(Color colorIn) {
        color = colorIn;
    }

    private int round;

    public int getRound() {
        return round;
    }

    public Rectangle round(int roundIn) {
        round = roundIn;
        return this;
    }

    public Rectangle(int xIn, int yIn, int widthIn, int heightIn, Color colorIn) {
        super(xIn, yIn);
        width = widthIn;
        height = heightIn;
        color = colorIn;
    }

    @Override
    public void draw(long nvgContext) {
        nvgBeginPath(nvgContext);
        NVGColor nvgColor = NvgColorUtil.color(nvgContext, color);
        if (round > 0) nvgRoundedRect(nvgContext, x, y, width, height, round);
        else nvgRect(nvgContext, x, y, width, height);
        nvgFill(nvgContext);
        nvgColor.free();
    }

    @Override
    public boolean contains(int anotherX, int anotherY) {
        return anotherX >= x && anotherX <= x + width && anotherY >= y && anotherY <= y + height;
    }
}
