package engine.assets;

import engine.gfx.Material;
import engine.gfx.Mesh;
import engine.gfx.Shader;
import engine.gfx.Texture;
import engine.scene.Entity;
import engine.scene.Scene;
import engine.util.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Model implements AutoCloseable {
    public record Part(String name, Mesh mesh, Color baseColor) {}

    private final String path;
    private final List<Part> parts;

    Model(String path, List<Part> parts) {
        this.path = path;
        this.parts = parts;
    }

    public String path() {
        return path;
    }

    public List<Part> parts() {
        return Collections.unmodifiableList(parts);
    }

    public Mesh mesh() {
        if (parts.isEmpty()) throw new IllegalStateException("Model has no meshes: " + path);
        return parts.get(0).mesh();
    }

    public Entity spawn(Scene scene, Shader shader) {
        return spawn(scene, shader, null).get(0);
    }

    public List<Entity> spawn(Scene scene, Shader shader, Texture texture) {
        List<Entity> out = new ArrayList<>();
        for (Part p : parts) {
            Material mat = new Material(shader, texture).tint(p.baseColor().toVector().x, p.baseColor().toVector().y, p.baseColor().toVector().z);
            out.add(scene.add(new Entity(p.mesh(), mat).named(p.name())));
        }
        return out;
    }

    @Override
    public void close() throws Exception {
        for (Part p : parts) p.mesh().close();
        parts.clear();
    }
}
