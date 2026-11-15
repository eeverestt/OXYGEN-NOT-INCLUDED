package engine.core;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Window implements AutoCloseable {
    private final long handle;
    private int fbWidth, fbHeight;

    public Window(String title, int width, int height, boolean vsync) {
        GLFWErrorCallback.createPrint(System.err).set();
        if (Platform.get() == Platform.LINUX) glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_X11);
        if (!glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        handle = glfwCreateWindow(width, height, title, NULL, NULL);
        if (handle == NULL) throw new RuntimeException("Failed to create GLFW window");

        GLFWVidMode vm = glfwGetVideoMode(glfwGetPrimaryMonitor());

        if (vm != null && glfwGetPlatform() != GLFW_PLATFORM_WAYLAND)
            glfwSetWindowPos(handle, (vm.width() - width) / 2, (vm.height() - height) / 2);

        glfwMakeContextCurrent(handle);
        GL.createCapabilities();
        glfwSwapInterval(vsync ? 1 : 0);

        try (MemoryStack s = stackPush()) {
            IntBuffer w = s.mallocInt(1), h = s.mallocInt(1);
            glfwGetFramebufferSize(handle, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }
        glfwSetFramebufferSizeCallback(handle, (win, w, h) -> {
            fbWidth = w;
            fbHeight = h;
            glViewport(0, 0, w, h);
        });

        Input.init(handle);
        glViewport(0, 0, fbWidth, fbHeight);
        glfwShowWindow(handle);
    }

    public void poll() {
        Input.beginFrame();
        glfwPollEvents();
    }

    public void clear(float r, float g, float b) {
        glClearColor(r, g, b, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    }

    public void swap() { glfwSwapBuffers(handle); }
    public boolean shouldClose() { return glfwWindowShouldClose(handle); }
    public void requestClose() { glfwSetWindowShouldClose(handle, true); }
    public void setTitle(String title) { glfwSetWindowTitle(handle, title); }

    public int width() { return fbWidth; }
    public int height() { return fbHeight; }
    public float aspect() { return fbHeight == 0 ? 1f : (float) fbWidth / fbHeight; }
    public long handle() { return handle; }

    @Override
    public void close() {
        org.lwjgl.glfw.Callbacks.glfwFreeCallbacks(handle);
        glfwDestroyWindow(handle);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}
