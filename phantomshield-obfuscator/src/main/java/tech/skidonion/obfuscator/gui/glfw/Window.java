package tech.skidonion.obfuscator.gui.glfw;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import tech.skidonion.obfuscator.gui.glfw.input.mouse.MouseManager;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window {

    public static Window INSTANCE = new Window();

    private long window = NULL;

    public long getWindow() {
        return window;
    }

    public void setWindowIcon(ByteBuffer icon16, ByteBuffer icon32) {
        try (MemoryStack stack = stackPush()) {
            final IntBuffer w = stack.mallocInt(1);
            final IntBuffer h = stack.mallocInt(1);
            final IntBuffer comp = stack.mallocInt(1);

            try (GLFWImage.Buffer icons = GLFWImage.malloc(2)) {
                ByteBuffer pixel16 = stbi_load_from_memory(icon16, w, h, comp, 4);
                icons.position(0).width(w.get(0)).height(h.get(0)).pixels(pixel16);

                ByteBuffer pixel32 = stbi_load_from_memory(icon32, w, h, comp, 4);
                icons.position(1).width(w.get(0)).height(h.get(0)).pixels(pixel32);

                icons.position(0);
                glfwSetWindowIcon(window, icons);

                stbi_image_free(pixel32);
                stbi_image_free(pixel16);
            }
        }
    }

    private String windowTitle = "PhantomShield-X";

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitleIn) {
        windowTitle = windowTitleIn;
        if (isCreated()) {
            glfwSetWindowTitle(window, windowTitle);
        }
    }

    private int windowWidth = 800;

    public int getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(int windowWidthIn) {
        windowWidth = windowWidthIn;
    }

    private int windowHeight = 400;

    public int getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(int windowHeightIn) {
        windowHeight = windowHeightIn;
    }

    public boolean isCreated() {
        return window != NULL;
    }

    public boolean isVisible() {
        return isCreated() && glfwGetWindowAttrib(window, GLFW_VISIBLE) == GLFW_TRUE;
    }

    private Window() {
    }

    public boolean init() {
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        window = glfwCreateWindow(windowWidth, windowHeight, windowTitle, NULL, NULL);
        if (window != NULL) {
            final GLFWVidMode videoMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            if (videoMode != null) {
                glfwSetWindowPos(window, (videoMode.width() - windowWidth) / 2, (videoMode.height() - windowHeight) / 2);
            }

            glfwSetScrollCallback(window, new GLFWScrollCallback() {
                @Override
                public void invoke(long l, double v, double v1) {
                    MouseManager.INSTANCE.onScroll((int) v, (int) v1);
                }
            });

            glfwSetCursorPosCallback(window, new GLFWCursorPosCallback() {
                @Override
                public void invoke(long l, double v, double v1) {
                    MouseManager.INSTANCE.onCursorPos((int) v, (int) v1);
                }
            });

            glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
                @Override
                public void invoke(long l, int i, int i1, int i2) {
                    MouseManager.INSTANCE.onMouseClick(i, i1);
                }
            });
            return true;
        }
        return false;
    }

    public void showWindow() {
        glfwShowWindow(window);
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        GL.createCapabilities();
    }
}
