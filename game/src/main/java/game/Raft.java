package game;

import engine.gfx.Material;
import engine.gfx.Mesh;
import engine.gfx.Shader;
import engine.gfx.Texture;
import engine.scene.Entity;
import engine.scene.Scene;

public class Raft implements AutoCloseable {
    public final float width = 2f;
    public final float length = 80f / 24f;
    public final float thickness = 5f / 24f;

    public final Entity deck;
    public final Entity sides;

    private final Mesh deckMesh;
    private final Mesh sidesMesh;

    public Raft(Scene scene, Shader shader, Texture deckTexture, Texture sideTexture, float x, float z) {
        float w = width / 2f;
        float l = length / 2f;
        float t = thickness / 2f;

        deckMesh = new Mesh(new float[]{
                -w,  t,  l,   0,  1, 0,   0, 0,
                 w,  t,  l,   0,  1, 0,   1, 0,
                 w,  t, -l,   0,  1, 0,   1, 1,
                -w,  t, -l,   0,  1, 0,   0, 1,

                -w, -t, -l,   0, -1, 0,   0, 0,
                 w, -t, -l,   0, -1, 0,   1, 0,
                 w, -t,  l,   0, -1, 0,   1, 1,
                -w, -t,  l,   0, -1, 0,   0, 1,
        }, new int[]{
                0, 1, 2, 0, 2, 3,
                4, 5, 6, 4, 6, 7
        });

        sidesMesh = new Mesh(new float[]{
                 w, -t, -l,   0, 0, -1,   0, 0,
                -w, -t, -l,   0, 0, -1,   1, 0,
                -w,  t, -l,   0, 0, -1,   1, 1,
                 w,  t, -l,   0, 0, -1,   0, 1,

                -w, -t,  l,   0, 0,  1,   0, 0,
                 w, -t,  l,   0, 0,  1,   1, 0,
                 w,  t,  l,   0, 0,  1,   1, 1,
                -w,  t,  l,   0, 0,  1,   0, 1,

                 w, -t,  l,   1, 0,  0,   0, 0,
                 w, -t, -l,   1, 0,  0,   1, 0,
                 w,  t, -l,   1, 0,  0,   1, 1,
                 w,  t,  l,   1, 0,  0,   0, 1,

                -w, -t, -l,  -1, 0,  0,   0, 0,
                -w, -t,  l,  -1, 0,  0,   1, 0,
                -w,  t,  l,  -1, 0,  0,   1, 1,
                -w,  t, -l,  -1, 0,  0,   0, 1,
        }, new int[]{
                0, 1, 2, 0, 2, 3,
                4, 5, 6, 4, 6, 7,
                8, 9, 10, 8, 10, 11,
                12, 13, 14, 12, 14, 15
        });

        deck = scene.add(new Entity(deckMesh, new Material(shader, deckTexture)).named("raft").at(x, 0f, z));
        sides = scene.add(new Entity(sidesMesh, new Material(shader, sideTexture)).named("raft sides").at(x, 0f, z));
    }

    public void update(float time, float seaLevel) {
        float x = deck.transform.position.x;
        float z = deck.transform.position.z;
        float front = Waves.height(x, z - length / 2f, time);
        float back = Waves.height(x, z + length / 2f, time);
        float left = Waves.height(x - width / 2f, z, time);
        float right = Waves.height(x + width / 2f, z, time);

        float water = (front + back + left + right) / 4f;
        deck.transform.position.y = seaLevel + water + 0.05f;
        deck.transform.setEuler(
                (float) Math.atan2(front - back, length),
                0f,
                (float) Math.atan2(right - left, width)
        );

        sides.transform.position.set(deck.transform.position);
        sides.transform.rotation.set(deck.transform.rotation);
    }

    @Override
    public void close() {
        deckMesh.close();
        sidesMesh.close();
    }
}
