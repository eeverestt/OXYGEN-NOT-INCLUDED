package engine.scene;

import engine.gfx.Material;
import engine.gfx.Mesh;

public final class Entity {
    public String name = "entity";
    public final Transform transform = new Transform();
    public Mesh mesh;
    public Material material;
    public boolean visible = true;
    public boolean transparent = false;

    public Entity(Mesh mesh, Material material) {
        this.mesh = mesh;
        this.material = material;
    }

    public Entity named(String name) {
        this.name = name;
        return this;
    }

    public Entity at(float x, float y, float z) {
        transform.setPosition(x, y, z);
        return this;
    }

    public Entity scaled(float scale) {
        transform.setScale(scale);
        return this;
    }

    public Entity transparent() {
        this.transparent = true;
        return this;
    }

    public float worldRadius() {
        return mesh.radius * Math.max(transform.scale.x, Math.max(transform.scale.y, transform.scale.z));
    }
}