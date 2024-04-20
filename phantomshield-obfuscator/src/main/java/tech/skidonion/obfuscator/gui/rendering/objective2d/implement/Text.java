package tech.skidonion.obfuscator.gui.rendering.objective2d.implement;

import org.lwjgl.nanovg.NVGColor;
import tech.skidonion.obfuscator.gui.rendering.objective2d.Objective2D;
import tech.skidonion.obfuscator.gui.utils.NvgColorUtil;

import java.awt.*;

import static org.lwjgl.nanovg.NanoVG.*;

public class Text extends Objective2D {

    private String string;

    public String getString() {
        return string;
    }

    public void setString(String stringIn) {
        string = stringIn;
    }

    private String font;

    public String getFont() {
        return font;
    }

    public void setFont(String fontIn) {
        font = fontIn;
    }

    private float size;

    public float getSize() {
        return size;
    }

    public void setSize(float sizeIn) {
        size = sizeIn;
    }

    private int align;

    public int getAlign() {
        return align;
    }

    public void setAlign(int alignIn) {
        align = alignIn;
    }

    private Color color;

    public Color getColor() {
        return color;
    }

    public void setColor(Color colorIn) {
        color = colorIn;
    }

    public Text(int xIn, int yIn, String stringIn, String fontIn, float sizeIn, int alignIn, Color colorIn) {
        super(xIn, yIn);
        string = stringIn;
        font = fontIn;
        size = sizeIn;
        align = alignIn;
        color = colorIn;
    }

    @Override
    public void draw(long nvgContext) {
        nvgBeginPath(nvgContext);
        nvgFontSize(nvgContext, size);
        nvgFontFace(nvgContext, font);
        nvgTextAlign(nvgContext, align);
        NVGColor nvgColor = NvgColorUtil.color(nvgContext, color);
        nvgText(nvgContext, x, y, string);
        nvgColor.free();
    }

    @Override
    public boolean contains(int anotherX, int anotherY) {
        return false;
    }
}
