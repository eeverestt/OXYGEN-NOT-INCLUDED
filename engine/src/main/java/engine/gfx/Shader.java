package engine.gfx;

import engine.assets.Resources;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Shader implements AutoCloseable {
    private final int program;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public Shader(String vertexSource, String fragmentSource) {
        int vs = compile(GL_VERTEX_SHADER, vertexSource);
        int fs = compile(GL_FRAGMENT_SHADER, fragmentSource);
        program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Shader link failed:\n" + glGetProgramInfoLog(program));
        glDetachShader(program, vs);
        glDetachShader(program, fs);
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    public static Shader load(String vertPath, String fragPath) {
        return new Shader(Resources.readText(vertPath), Resources.readText(fragPath));
    }

    private static int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);
        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String kind = type == GL_VERTEX_SHADER ? "vertex" : "fragment";
            throw new RuntimeException("Failed to compile " + kind + " shader:\n" + glGetShaderInfoLog(id));
        }
        return id;
    }

    public Shader bind() { glUseProgram(program); return this; }
    public void unbind() { glUseProgram(0); }

    private int loc(String name) {
        return uniforms.computeIfAbsent(name, n -> glGetUniformLocation(program, n)); // -1 is silently ignored by GL
    }

    public Shader set(String n, int v)     { glUniform1i(loc(n), v); return this; }
    public Shader set(String n, float v)   { glUniform1f(loc(n), v); return this; }
    public Shader set(String n, boolean v) { glUniform1i(loc(n), v ? 1 : 0); return this; }
    public Shader set(String n, Vector2f v) { glUniform2f(loc(n), v.x, v.y); return this; }
    public Shader set(String n, Vector3f v) { glUniform3f(loc(n), v.x, v.y, v.z); return this; }
    public Shader set(String n, Vector4f v) { glUniform4f(loc(n), v.x, v.y, v.z, v.w); return this; }

    public Shader set(String n, Matrix4f m) {
        try (MemoryStack s = stackPush()) {
            FloatBuffer fb = s.mallocFloat(16);
            glUniformMatrix4fv(loc(n), false, m.get(fb));
        }
        return this;
    }

    public Shader set(String n, Matrix3f m) {
        try (MemoryStack s = stackPush()) {
            FloatBuffer fb = s.mallocFloat(9);
            glUniformMatrix3fv(loc(n), false, m.get(fb));
        }
        return this;
    }

    @Override
    public void close() { glDeleteProgram(program); }
}
