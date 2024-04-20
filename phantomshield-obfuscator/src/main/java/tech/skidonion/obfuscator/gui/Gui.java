package tech.skidonion.obfuscator.gui;

import org.lwjgl.glfw.GLFWErrorCallback;
import tech.skidonion.obfuscator.gui.glfw.Window;
import tech.skidonion.obfuscator.gui.rendering.nvg.NvgRenderer;
import tech.skidonion.obfuscator.gui.rendering.screen.Screen;
import tech.skidonion.obfuscator.gui.rendering.screen.implement.InitialScreen;
import tech.skidonion.obfuscator.gui.utils.ResourceUtil;

import javax.swing.*;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public final class Gui {

    public static Gui INSTANCE = new Gui();

    private static final boolean RELEASE = false;

    private Screen screen;

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screenIn) {
        if (screenIn != screen) {

            if (screen != null) {
                screen.onClose();
            }

            screen = screenIn;
            screen.onInit();
        }
    }

    private Gui() {
    }

    public boolean init() {
        if (RELEASE) {
            JOptionPane.showMessageDialog(null, "GUI currently not available", "PhantomShield-X", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        setScreen(new InitialScreen());
        return initGlfw() && initWindow() && initNvgRenderer();
    }

    private boolean initGlfw() {
        GLFWErrorCallback.createPrint(System.err);
        if (glfwInit()) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                glfwTerminate();
                Objects.requireNonNull(glfwSetErrorCallback(null)).free();
            }));
            return true;
        } else {
            return false;
        }
    }

    private boolean initWindow() {
        if (Window.INSTANCE.init()) {
            Window.INSTANCE.setWindowIcon(ResourceUtil.resourceToByteBuffer("/gui/icon/png/0.png", getClass()), ResourceUtil.resourceToByteBuffer("/gui/icon/png/1.png", getClass()));
            Window.INSTANCE.showWindow();
            return true;
        }
        return false;
    }

    private boolean initNvgRenderer() {
        return NvgRenderer.INSTANCE.init();
    }

    public void runGuiLoop() {

        final Window window = Window.INSTANCE;
        final long handle = window.getWindow();

        while (!glfwWindowShouldClose(handle)) {

            glViewport(0, 0, window.getWindowWidth(), window.getWindowHeight());
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            NvgRenderer.INSTANCE.setup();

            screen.draw(NvgRenderer.INSTANCE.getNvgContext());

            NvgRenderer.INSTANCE.end();

            glfwSwapBuffers(handle);
            glfwPollEvents();
        }
        NvgRenderer.INSTANCE.destroy();
        glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
    }
}
