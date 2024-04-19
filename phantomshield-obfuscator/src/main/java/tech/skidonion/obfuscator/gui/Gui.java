package tech.skidonion.obfuscator.gui;

import org.lwjgl.glfw.GLFWErrorCallback;
import tech.skidonion.obfuscator.gui.objective2d.implement.Rectangle;
import tech.skidonion.obfuscator.gui.utility.NvgRenderUtil;
import tech.skidonion.obfuscator.gui.window.Window;

import java.awt.*;
import java.util.Objects;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.opengl.GL11.*;
import static tech.skidonion.obfuscator.gui.window.Window.*;

public final class Gui {

    private Gui() {
    }

    public static Gui initGui() {
        return initGLFW() && initWindow() && initNvg() ? new Gui() : null;
    }

    private static boolean initGLFW() {
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

    private static boolean initWindow() {
        Window.setWindowTitle("PhantomShield-X");
        return Window.createWindow();
    }

    private static boolean initNvg() {
        return Window.createNvgContext();
    }

    public void loop() {
        while (!glfwWindowShouldClose(getHandle())) {
            glViewport(0, 0, getWindowWidth(), getWindowHeight());
            glClearColor(0F, 0F, 0F, 1F);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            NvgRenderUtil.setup(NvgRenderUtil.Task.GRAPHICS_2D , getNvgContext());

            // THIS IS JUST A RENDERING TEST, I WILL IMPLEMENT MORE FUNCTIONALITY SOON
            new Rectangle(10, 10, 200, 200, Color.WHITE).draw(getNvgContext());

            NvgRenderUtil.end(NvgRenderUtil.Task.GRAPHICS_2D , getNvgContext());

            glfwSwapBuffers(getHandle());
            glfwPollEvents();
        }
        glfwFreeCallbacks(getHandle());
        glfwDestroyWindow(getHandle());
    }
}
