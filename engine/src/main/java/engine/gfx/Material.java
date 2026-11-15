package engine.gfx;

import org.joml.Vector3f;

public final class Material {
    private static Texture whiteTexture;

    public final Shader shader;
    public Texture texture;
    public final Vector3f tint = new Vector3f(1, 1, 1);

    public Material(Shader shader, Texture texture) {
        this.shader = shader;
        this.texture = texture;
    }

    public Material(Shader shader) {
        this(shader, null);
    }

    public void apply() {
        (texture != null ? texture : white()).bind(0);
        shader.set("uTex", 0).set("uTint", tint);
    }

    public Material tint(float r, float g, float b) {
        tint.set(r, g, b); return this;
    }

    private static Texture white() {
        if (whiteTexture == null) whiteTexture = Texture.solid(255, 255, 255);
        return whiteTexture;
    }
}
