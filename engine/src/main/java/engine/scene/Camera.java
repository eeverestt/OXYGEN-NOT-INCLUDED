package engine.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Camera {
    public final Vector3f position = new Vector3f(0, 0, 3);
    public float yaw = -90f;   // degrees; -90 looks down -Z
    public float pitch = 0f;   // degrees
    public float fov = 70f;    // vertical FOV in degrees
    public float near = 0.1f, far = 500f;
    public float lookSensitivity = 0.1f;

    private final Vector3f forward = new Vector3f();
    private final Vector3f right = new Vector3f();
    private final Vector3f up = new Vector3f(0, 1, 0);
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f proj = new Matrix4f();
    private final Vector3f tmp = new Vector3f();

    public Camera() { updateVectors(); }

    private void updateVectors() {
        double y = Math.toRadians(yaw), p = Math.toRadians(pitch);
        forward.set((float) (Math.cos(y) * Math.cos(p)), (float) Math.sin(p), (float) (Math.sin(y) * Math.cos(p))).normalize();
        forward.cross(0, 1, 0, right).normalize();
    }

    public void look(float dx, float dy) {
        yaw += dx * lookSensitivity;
        pitch = Math.max(-89f, Math.min(89f, pitch - dy * lookSensitivity));
        updateVectors();
    }

    public void moveForward(float d) { position.add(tmp.set(forward).mul(d)); }
    public void moveRight(float d)   { position.add(tmp.set(right).mul(d)); }
    public void moveUp(float d)      { position.y += d; }

    public void moveForwardFlat(float d) {
        tmp.set(forward.x, 0, forward.z).normalize().mul(d);
        position.add(tmp);
    }

    public Vector3f forward() { return forward; }
    public Vector3f right()   { return right; }

    public Matrix4f view() {
        return view.identity().lookAt(position, tmp.set(position).add(forward), up);
    }

    public Matrix4f projection(float aspect) {
        return proj.identity().perspective((float) Math.toRadians(fov), aspect, near, far);
    }
}
