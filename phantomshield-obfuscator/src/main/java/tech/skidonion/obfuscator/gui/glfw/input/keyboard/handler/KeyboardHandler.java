package tech.skidonion.obfuscator.gui.glfw.input.keyboard.handler;

public abstract class KeyboardHandler {

    public abstract void onCharInput(char character);

    public abstract void onKeyPress(char character, int code);
}
