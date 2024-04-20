package tech.skidonion.obfuscator.gui.glfw.input.mouse.handler;

public abstract class MouseHandler {

    public abstract void onScroll(int x, int y);

    public abstract void onCursorPos(int x, int y);

    public abstract void onMouseClick(int button, int state);
}
