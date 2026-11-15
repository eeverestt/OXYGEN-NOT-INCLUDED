package engine.scene;

import engine.util.Color;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Scene {
    public final Camera camera = new Camera();
    public final Vector3f lightDir = new Vector3f(-0.4f, -1f, -0.3f);
    public final Vector3f clearColor = new Color(0x696568).toNormalisedVector();

    private final List<Entity> entities = new ArrayList<>();

    public Entity add(Entity e) {
        entities.add(e);
        return e;
    }

    public void remove(Entity e) { entities.remove(e); }

    public void clear() { entities.clear(); }

    public List<Entity> entities() { return Collections.unmodifiableList(entities); }
}