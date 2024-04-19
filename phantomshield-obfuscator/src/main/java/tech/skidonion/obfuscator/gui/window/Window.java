package tech.skidonion.obfuscator.gui.window;

import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.nanovg.NanoVGGL3.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window {

    private static long handle = NULL;

    public static long getHandle() {
        return handle;
    }

    private static long nvgContext = NULL;

    public static long getNvgContext() {
        return nvgContext;
    }

    private static int windowWidth = 1024;

    public static int getWindowWidth() {
        return windowWidth;
    }

    public static void setWindowWidth(int windowWidthIn) {
        windowWidth = windowWidthIn;
        if (isCreated()) {
            glfwSetWindowSize(handle, windowWidth, windowHeight);
        }
    }

    private static int windowHeight = 680;

    public static int getWindowHeight() {
        return windowHeight;
    }

    public static void setWindowHeight(int windowHeightIn) {
        windowHeight = windowHeightIn;
        if (isCreated()) {
            glfwSetWindowSize(handle, windowWidth, windowHeight);
        }
    }

    private static String windowTitle = "";

    public static String getWindowTitle() {
        return windowTitle;
    }

    public static void setWindowTitle(String windowTitleIn) {
        windowTitle = windowTitleIn;
        if (isCreated()) {
            glfwSetWindowTitle(handle, windowTitle);
        }
    }

    public static boolean isCreated() {
        return handle != NULL;
    }

    public static boolean createWindow() {
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        handle = glfwCreateWindow(windowWidth, windowHeight, windowTitle, NULL, NULL);
        if (handle == NULL) return false;

        final GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        if (videoMode != null) {
            glfwSetWindowPos(handle, (videoMode.width() - windowWidth) / 2, (videoMode.height() - windowHeight) / 2);
        }

        setWindowTitle(getWindowTitle());

        glfwShowWindow(handle);

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();

        return true;
    }

    public static boolean createNvgContext() {
        nvgContext = nvgCreate(NVG_ANTIALIAS | NVG_STENCIL_STROKES);
        return nvgContext != NULL;
    }
}
