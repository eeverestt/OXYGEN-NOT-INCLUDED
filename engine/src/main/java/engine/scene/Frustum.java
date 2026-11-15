package engine.scene;

import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Frustum {
    private final FrustumIntersection tester = new FrustumIntersection();
    private final Matrix4f viewProj = new Matrix4f();
    private final Matrix4f inverse = new Matrix4f();
    private final Vector3f[] corners = new Vector3f[8];

    private static final float[][] NDC = {
            {-1, -1, -1}, {1, -1, -1}, {1, 1, -1}, {-1, 1, -1},
            {-1, -1,  1}, {1, -1,  1}, {1, 1,  1}, {-1, 1,  1}
    };

    public static final int[][] EDGES = {
            {0, 1}, {1, 2}, {2, 3}, {3, 0},   // near
            {4, 5}, {5, 6}, {6, 7}, {7, 4},   // far
            {0, 4}, {1, 5}, {2, 6}, {3, 7}    // sides
    };

    public Frustum() {
        for (int i = 0; i < 8; i++) corners[i] = new Vector3f();
    }

    public void set(Matrix4f projection, Matrix4f view) {
        projection.mul(view, viewProj);
        tester.set(viewProj);
        viewProj.invert(inverse);
        for (int i = 0; i < 8; i++) {
            inverse.transformProject(NDC[i][0], NDC[i][1], NDC[i][2], corners[i]);
        }
    }

    public boolean testSphere(Vector3f c, float r) {
        return tester.testSphere(c.x, c.y, c.z, r);
    }

    public boolean testAabb(Vector3f min, Vector3f max) {
        return tester.testAab(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public Vector3f corner(int i) { return corners[i]; }
}