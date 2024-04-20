package tech.skidonion.obfuscator.gui.glfw.input.mouse;

import tech.skidonion.obfuscator.gui.glfw.input.mouse.handler.MouseHandler;

import java.util.ArrayList;
import java.util.List;

public final class MouseManager {

    public static MouseManager INSTANCE = new MouseManager();

    private final List<MouseHandler> registered = new ArrayList<>();

    public void register(MouseHandler mouseHandler) {
        registered.add(mouseHandler);
    }

    public void onScroll(int x, int y) {
        for (MouseHandler mouseHandler : registered) {
            mouseHandler.onScroll(x, y);
        }
    }

    public void onCursorPos(int x, int y) {
        for (MouseHandler mouseHandler : registered) {
            mouseHandler.onCursorPos(x, y);
        }
    }

    public void onMouseClick(int button, int state) {
        for (MouseHandler mouseHandler : registered) {
            mouseHandler.onMouseClick(button, state);
        }
    }
}
