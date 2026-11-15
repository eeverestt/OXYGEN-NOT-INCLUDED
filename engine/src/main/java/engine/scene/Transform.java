package engine.scene;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Transform {
    public final Vector3f position = new Vector3f();
    public final Quaternionf rotation = new Quaternionf();
    public final Vector3f scale = new Vector3f(1, 1, 1);
    private final Matrix4f matrix = new Matrix4f();

    public Transform() {}
    public Transform(float x, float y, float z) { position.set(x, y, z); }

    public Transform setPosition(float x, float y, float z) { position.set(x, y, z); return this; }
    public Transform setScale(float s) { scale.set(s); return this; }
    public Transform setScale(float x, float y, float z) { scale.set(x, y, z); return this; }

    public Transform rotate(float radians, float ax, float ay, float az) {
        rotation.rotateAxis(radians, ax, ay, az);
        return this;
    }

    public Transform setEuler(float pitch, float yaw, float roll) {
        rotation.identity().rotateYXZ(yaw, pitch, roll);
        return this;
    }

    public Matrix4f matrix() {
        return matrix.identity().translate(position).rotate(rotation).scale(scale);
    }
}
