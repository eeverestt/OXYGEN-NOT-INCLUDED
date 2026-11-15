package engine.core;

import static org.lwjgl.glfw.GLFW.*;

public final class Input {
    private static final boolean[] down = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] pressed = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] released = new boolean[GLFW_KEY_LAST + 1];
    private static final boolean[] mDown = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];
    private static final boolean[] mPressed = new boolean[GLFW_MOUSE_BUTTON_LAST + 1];

    private static double mouseX, mouseY, dx, dy, scroll;
    private static boolean firstMouse = true;
    private static long window;

    private Input() {}

    static void init(long win) {
        window = win;
        glfwSetKeyCallback(win, (w, key, sc, action, mods) -> {
            if (key < 0 || key > GLFW_KEY_LAST) return;
            if (action == GLFW_PRESS) { down[key] = true; pressed[key] = true; }
            else if (action == GLFW_RELEASE) { down[key] = false; released[key] = true; }
        });
        glfwSetMouseButtonCallback(win, (w, b, action, mods) -> {
            if (b < 0 || b > GLFW_MOUSE_BUTTON_LAST) return;
            if (action == GLFW_PRESS) { mDown[b] = true; mPressed[b] = true; }
            else if (action == GLFW_RELEASE) mDown[b] = false;
        });
        glfwSetCursorPosCallback(win, (w, x, y) -> {
            if (firstMouse) { mouseX = x; mouseY = y; firstMouse = false; }
            dx += x - mouseX;
            dy += y - mouseY;
            mouseX = x; mouseY = y;
        });
        glfwSetScrollCallback(win, (w, sx, sy) -> scroll += sy);
    }

    static void beginFrame() {
        java.util.Arrays.fill(pressed, false);
        java.util.Arrays.fill(released, false);
        java.util.Arrays.fill(mPressed, false);
        dx = dy = scroll = 0;
    }

    public static boolean isDown(int key) { return key >= 0 && key <= GLFW_KEY_LAST && down[key]; }
    public static boolean wasPressed(int key) { return key >= 0 && key <= GLFW_KEY_LAST && pressed[key]; }
    public static boolean wasReleased(int key) { return key >= 0 && key <= GLFW_KEY_LAST && released[key]; }

    public static boolean isMouseDown(int button) { return button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST && mDown[button]; }
    public static boolean wasMousePressed(int button) { return button >= 0 && button <= GLFW_MOUSE_BUTTON_LAST && mPressed[button]; }

    public static float mouseX() { return (float) mouseX; }
    public static float mouseY() { return (float) mouseY; }
    public static float mouseDX() { return (float) dx; }
    public static float mouseDY() { return (float) dy; }
    public static float scrollDelta() { return (float) scroll; }

    public static void captureMouse(boolean capture) {
        glfwSetInputMode(window, GLFW_CURSOR, capture ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
        firstMouse = true;
    }
}
