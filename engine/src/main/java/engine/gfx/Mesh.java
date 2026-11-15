package engine.gfx;

import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33C.*;

public final class Mesh implements AutoCloseable {
    /** position(3), normal(3), uv(2) to match the built-in shaders. */
    public static final int[] STANDARD = {3, 3, 2};

    private final int vao, vbo, ebo, indexCount;

    public final float radius;

    public final Vector3f boundCenter = new Vector3f();
    public float boundRadius;

    public Mesh(float[] vertices, int[] indices, int... layout) {
        indexCount = indices.length;
        int stride = 0;
        for (int a : layout) stride += a;

        float r2 = 0;
        if (layout.length > 0 && layout[0] >= 3) { // first attribute is assumed to be position
            for (int i = 0; i + 2 < vertices.length; i += stride) {
                float x = vertices[i], y = vertices[i + 1], z = vertices[i + 2];
                r2 = Math.max(r2, x * x + y * y + z * z);
            }
        }
        radius = (float) Math.sqrt(r2);

        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        ebo = glGenBuffers();

        glBindVertexArray(vao);

        FloatBuffer vb = MemoryUtil.memAllocFloat(vertices.length);
        try {
            vb.put(vertices).flip();
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, vb, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(vb);
        }

        IntBuffer ib = MemoryUtil.memAllocInt(indices.length);
        try {
            ib.put(indices).flip();
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);
        } finally {
            MemoryUtil.memFree(ib);
        }

        long offset = 0;
        for (int i = 0; i < layout.length; i++) {
            glEnableVertexAttribArray(i);
            glVertexAttribPointer(i, layout[i], GL_FLOAT, false, stride * Float.BYTES, offset * Float.BYTES);
            offset += layout[i];
        }

        glBindVertexArray(0);
    }

    public Mesh(float[] vertices, int[] indices) { this(vertices, indices, STANDARD); }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    @Override
    public void close() {
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        glDeleteVertexArrays(vao);
    }

    public void computeBounds(float[] verts, int stride) {
        Vector3f min = new Vector3f(Float.MAX_VALUE), max = new Vector3f(-Float.MAX_VALUE);
        for (int i = 0; i + 2 < verts.length; i += stride) {
            min.min(new Vector3f(verts[i], verts[i + 1], verts[i + 2]));
            max.max(new Vector3f(verts[i], verts[i + 1], verts[i + 2]));
        }
        min.add(max, boundCenter).mul(0.5f);
        boundRadius = boundCenter.distance(max);
    }

    public static Mesh cube() {
        float[][] faces = {
            { 1, 0, 0,   0, 0,-1,   0, 1, 0},
            {-1, 0, 0,   0, 0, 1,   0, 1, 0},
            { 0, 1, 0,   1, 0, 0,   0, 0,-1},
            { 0,-1, 0,   1, 0, 0,   0, 0, 1},
            { 0, 0, 1,   1, 0, 0,   0, 1, 0},
            { 0, 0,-1,  -1, 0, 0,   0, 1, 0},
        };
        float[] su = {-1, 1, 1, -1}, sv = {-1, -1, 1, 1};
        float[] tu = {0, 1, 1, 0},   tv = {0, 0, 1, 1};

        float[] verts = new float[6 * 4 * 8];
        int[] idx = new int[6 * 6];
        int vi = 0, ii = 0;
        for (int f = 0; f < 6; f++) {
            float[] F = faces[f];
            int base = f * 4;
            for (int c = 0; c < 4; c++) {
                for (int k = 0; k < 3; k++)
                    verts[vi++] = F[k] * 0.5f + F[3 + k] * 0.5f * su[c] + F[6 + k] * 0.5f * sv[c];
                verts[vi++] = F[0]; verts[vi++] = F[1]; verts[vi++] = F[2];
                verts[vi++] = tu[c]; verts[vi++] = tv[c];
            }
            idx[ii++] = base;     idx[ii++] = base + 1; idx[ii++] = base + 2;
            idx[ii++] = base;     idx[ii++] = base + 2; idx[ii++] = base + 3;
        }
        return new Mesh(verts, idx);
    }

    public static Mesh grid(int resolution, float size, float uvRepeat) {
        int verticesPerSide = resolution + 1;
        float[] vertices = new float[verticesPerSide * verticesPerSide * 8];
        int[] indices = new int[resolution * resolution * 6];

        int vi = 0;
        int ii = 0;

        for (int z = 0; z <= resolution; z++) {
            for (int x = 0; x <= resolution; x++) {
                float u = (float) x / resolution;
                float v = (float) z / resolution;

                vertices[vi++] = (u - 0.5f) * size;
                vertices[vi++] = 0;
                vertices[vi++] = (v - 0.5f) * size;

                vertices[vi++] = 0;
                vertices[vi++] = 1;
                vertices[vi++] = 0;

                vertices[vi++] = u * uvRepeat;
                vertices[vi++] = v * uvRepeat;
            }
        }

        for (int z = 0; z < resolution; z++) {
            for (int x = 0; x < resolution; x++) {
                int a = z * verticesPerSide + x;
                int b = a + 1;
                int d = a + verticesPerSide;
                int c = d + 1;

                indices[ii++] = a;
                indices[ii++] = d;
                indices[ii++] = b;

                indices[ii++] = b;
                indices[ii++] = d;
                indices[ii++] = c;
            }
        }

        return new Mesh(vertices, indices);
    }

    public static Mesh plane(float size, float uvRepeat) {
        float h = size / 2f;
        float[] v = {
            -h, 0,  h,  0, 1, 0,  0,        0,
             h, 0,  h,  0, 1, 0,  uvRepeat, 0,
             h, 0, -h,  0, 1, 0,  uvRepeat, uvRepeat,
            -h, 0, -h,  0, 1, 0,  0,        uvRepeat,
        };
        return new Mesh(v, new int[]{0, 1, 2, 0, 2, 3});
    }
}
