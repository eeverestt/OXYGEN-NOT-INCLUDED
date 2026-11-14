package demo;

import engine.gfx.*;
import engine.core.*;
import engine.scene.*;
import game.OceanAmbienceDemo;
import game.Player;
import game.Raft;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class Demo extends GameApp {
    private final Scene scene = new Scene();
    private final Renderer renderer = new Renderer();
    private final List<Entity> spinners = new ArrayList<>();

    private Shader shader;
    private Shader oceanShader;

    private Mesh cubeMesh;
    private Mesh oceanMesh;

    private Texture checker;
    private Texture raftTop;
    private Texture raftSide;

    private Entity oceanEntity;
    private Raft raft;
    private OceanAmbienceDemo oceanSound;

    private final Player player = new Player();
    private boolean flying;

    private float frameTime;

    @Override
    protected void init() {
        shader = Shader.load("shaders/engine/basic.vert", "shaders/engine/basic.frag");
        oceanShader = Shader.load("shaders/game/ocean.vert", "shaders/game/ocean.frag");

        cubeMesh = Mesh.cube();
        oceanMesh = Mesh.grid(256, 200.0f, 1.0f);

        Material oceanMaterial = new Material(oceanShader)
                .tint(1.0f, 1.0f, 1.0f);

        oceanEntity = new Entity(oceanMesh, oceanMaterial);
        oceanEntity.transform.position.set(0.0f, -1.0f, 0.0f);

        scene.add(oceanEntity);

        checker = Texture.checker(
                8,
                8,
                new int[]{230, 120, 40},
                new int[]{250, 220, 180}
        );

        for (int i = 0; i < 5; i++) {
            Material mat = new Material(shader, checker)
                    .tint(0.6f + 0.1f * i, 1f, 1.4f - 0.2f * i);

            spinners.add(scene.add(
                    new Entity(cubeMesh, mat)
                            .named("cube" + i)
                            .at(i * 2.5f - 5f, 1f, -4f)
            ));
        }

        raftTop = Texture.load("textures/raft_topandbottom.png", true);
        raftSide = Texture.load("textures/raft_side.png", true);
        raft = new Raft(scene, shader, raftTop, raftSide, 0f, -12f);
        raft.update(0f, oceanEntity.transform.position.y);
        player.position.set(raft.deck.transform.position).add(0f, 0.5f, 0f);
        oceanSound = new OceanAmbienceDemo();

        scene.camera.position.set(0, 2, 6);
        Input.captureMouse(true);
    }

    @Override
    protected void update(float dt) {
        frameTime = (float) glfwGetTime();

        if (Input.wasPressed(GLFW_KEY_ESCAPE)) {
            window.requestClose();
        }

        Camera cam = scene.camera;
        cam.look(Input.mouseDX(), Input.mouseDY());

        if (Input.wasPressed(GLFW_KEY_F)) {
            flying = !flying;
            if (!flying) player.position.set(cam.position).sub(0f, player.eyeHeight, 0f);
        }

        raft.update(frameTime, oceanEntity.transform.position.y);

        if (flying) {
            float speed = (Input.isDown(GLFW_KEY_LEFT_SHIFT) ? 12f : 5f) * dt;

            if (Input.isDown(GLFW_KEY_W)) cam.moveForward(speed);
            if (Input.isDown(GLFW_KEY_S)) cam.moveForward(-speed);
            if (Input.isDown(GLFW_KEY_D)) cam.moveRight(speed);
            if (Input.isDown(GLFW_KEY_A)) cam.moveRight(-speed);
            if (Input.isDown(GLFW_KEY_SPACE)) cam.moveUp(speed);
            if (Input.isDown(GLFW_KEY_LEFT_CONTROL)) cam.moveUp(-speed);
        } else {
            player.update(dt, cam, raft, oceanEntity.transform.position.y, frameTime);
        }

        audio.listenTo(cam);
        oceanSound.update(raft);

        for (int i = 0; i < spinners.size(); i++) {
            spinners.get(i).transform.rotate(
                    dt * (0.5f + i * 0.4f),
                    0.3f,
                    1f,
                    0.2f
            );
        }
    }

    @Override
    protected void render() {
        renderer.render(scene, window.width(), window.height(), frameTime);
    }

    @Override
    protected void cleanup() {
        if (shader != null) shader.close();
        if (oceanShader != null) oceanShader.close();

        if (cubeMesh != null) cubeMesh.close();
        if (oceanMesh != null) oceanMesh.close();

        if (checker != null) checker.close();
        if (raftTop != null) raftTop.close();
        if (raftSide != null) raftSide.close();

        if (raft != null) raft.close();
        if (oceanSound != null) oceanSound.close();

        renderer.close();
    }

    public static void main(String[] args) {
        new Demo().run("Abyssaline", 1280, 720);
    }
}