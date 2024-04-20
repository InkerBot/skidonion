package tech.skidonion.obfuscator.gui.glfw.input.keyboard;

import tech.skidonion.obfuscator.gui.glfw.input.keyboard.handler.KeyboardHandler;

import java.util.ArrayList;
import java.util.List;

public final class KeyboardManager {

    public static KeyboardManager INSTANCE = new KeyboardManager();

    private final List<KeyboardHandler> registered = new ArrayList<>();

    public void register(KeyboardHandler mouseHandler) {
        registered.add(mouseHandler);
    }

    public void fire(char character) {
        for (KeyboardHandler keyboardHandler : registered) {
            keyboardHandler.onCharInput(character);
        }
    }

    public void fire(char character, int code) {
        for (KeyboardHandler keyboardHandler : registered) {
            keyboardHandler.onKeyPress(character, code);
        }
    }
}
