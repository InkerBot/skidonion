package tech.skidonion.obfuscator.gui.objective2d.implement;

import org.lwjgl.nanovg.NVGColor;
import tech.skidonion.obfuscator.gui.objective2d.Objective2D;
import tech.skidonion.obfuscator.gui.utility.NvgColorUtil;

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

    public Rectangle(int xIn, int yIn, int widthIn, int heightIn, Color colorIn) {
        super(xIn, yIn);
        width = widthIn;
        height = heightIn;
        color = colorIn;
    }

    @Override
    public void draw(long nvgContext) {
        try (NVGColor nvgColor = NvgColorUtil.create(color)) {
            nvgBeginPath(nvgContext);
            nvgFillColor(nvgContext, nvgColor);
            nvgRect(nvgContext, x, y, width, height);
            nvgFill(nvgContext);
        }
    }

    @Override
    public boolean contains(int anotherX, int anotherY) {
        return false;
    }
}
