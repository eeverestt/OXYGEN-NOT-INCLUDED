package engine.gfx;

import engine.scene.Scene;
import org.joml.Matrix4f;
import org.joml.Vector2f;

public final class FrameContext {
    public Scene scene;
    public Matrix4f view, proj;
    public final Matrix4f invViewProj = new Matrix4f();
    public final Vector2f screenSize = new Vector2f();
    public float time;
    public int width, height;
}