package engine.core;

import engine.audio.Audio;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL33C.*;

public abstract class GameApp {
    protected Window window;
    protected Audio audio;
    protected float clearR = 0.08f, clearG = 0.09f, clearB = 0.12f;

    protected abstract void init();
    protected abstract void update(float dt);
    protected abstract void render();
    protected void cleanup() {}

    public final void run(String title, int width, int height) {
        window = new Window(title, width, height, true);
        try {
            audio = new Audio();
            glEnable(GL_DEPTH_TEST);
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);
            glFrontFace(GL_CCW);

            init();

            double last = glfwGetTime();
            while (!window.shouldClose()) {
                double now = glfwGetTime();
                float dt = (float) Math.min(now - last, 0.1); // clamp to avoid huge steps after a stall
                last = now;

                window.poll();
                update(dt);
                window.clear(clearR, clearG, clearB);
                render();
                window.swap();
            }
            cleanup();
        } finally {
            if (audio != null) audio.close();
            window.close();
        }
    }
}
