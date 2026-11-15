package engine.gfx;

import engine.scene.Entity;
import engine.scene.Scene;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.lwjgl.opengl.GL33C.*;

public final class Renderer implements AutoCloseable {
    private static final int DEPTH_UNIT = 2;

    private Framebuffer sceneFb, depthCopy, pingA, pingB;
    private int fullscreenVao;

    private final List<PostEffect> effects = new ArrayList<>();
    private final List<Entity> opaque = new ArrayList<>();
    private final List<Entity> transparent = new ArrayList<>();
    private final FrameContext frame = new FrameContext();
    private final Vector3f camPos = new Vector3f();
    private final Comparator<Entity> backToFront = (a, b) -> Float.compare(
            b.transform.position.distanceSquared(camPos),
            a.transform.position.distanceSquared(camPos));

    public List<PostEffect> effects() { return effects; }

    public void render(Scene scene, int width, int height, float time) {
        if (width <= 0 || height <= 0) return;

        ensureInit();
        ensureTargets(width, height);

        frame.scene = scene;
        frame.view = scene.camera.view();
        frame.proj = scene.camera.projection((float) width / height);
        frame.invViewProj.set(frame.proj).mul(frame.view).invert();
        frame.screenSize.set(width, height);
        frame.time = time;
        frame.width = width;
        frame.height = height;
        camPos.set(scene.camera.position);

        sortEntities(scene);

        sceneFb.bind();
        glClearColor(scene.clearColor.x, scene.clearColor.y, scene.clearColor.z, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        drawList(opaque, false);

        if (!transparent.isEmpty()) {
            sceneFb.copyDepthTo(depthCopy);
            sceneFb.bind();

            glActiveTexture(GL_TEXTURE0 + DEPTH_UNIT);
            glBindTexture(GL_TEXTURE_2D, depthCopy.depthTex);
            glActiveTexture(GL_TEXTURE0);

            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            drawList(transparent, true);
            glDisable(GL_BLEND);
        }
        sceneFb.unbind();

        postProcess(width, height);
    }


    private void sortEntities(Scene scene) {
        opaque.clear();
        transparent.clear();
        for (Entity e : scene.entities()) {
            if (!e.visible) continue;
            (e.transparent ? transparent : opaque).add(e);
        }
        if (transparent.size() > 1) transparent.sort(backToFront);
    }

    private void drawList(List<Entity> list, boolean transparentPass) {
        Scene scene = frame.scene;
        Shader last = null;
        for (Entity e : list) {
            Material m = e.material;
            Shader s = m.shader;

            if (s != last) {
                s.bind()
                        .set("uView", frame.view)
                        .set("uProj", frame.proj)
                        .set("uLightDir", scene.lightDir)
                        .set("uViewPos", scene.camera.position)
                        .set("uTime", frame.time);
                if (transparentPass) {
                    s.set("uSceneDepth", DEPTH_UNIT)
                            .set("uScreenSize", frame.screenSize)
                            .set("uInvViewProj", frame.invViewProj)
                            .set("uNear", scene.camera.near)
                            .set("uFar", scene.camera.far);
                }
                last = s;
            }

            m.apply();
            s.set("uModel", e.transform.matrix());
            e.mesh.draw();
        }
        if (last != null) last.unbind();
    }

    private void postProcess(int w, int h) {
        int enabled = 0;
        for (PostEffect e : effects) if (e.enabled) enabled++;

        if (enabled == 0) {
            sceneFb.blitColorToScreen();
            return;
        }
        if (enabled > 1 && pingA == null) {
            pingA = Framebuffer.colorOnly(w, h);
            pingB = Framebuffer.colorOnly(w, h);
        }

        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);

        Framebuffer src = sceneFb;
        int done = 0;
        for (PostEffect e : effects) {
            if (!e.enabled) continue;
            boolean last = ++done == enabled;

            Framebuffer dst = null;
            if (!last) dst = (src == pingA) ? pingB : pingA;

            if (dst != null) {
                dst.bind();
            } else {
                glBindFramebuffer(GL_FRAMEBUFFER, 0);
                glViewport(0, 0, w, h);
            }

            e.shader.bind();
            e.configure(frame);
            e.shader.set("uColorTex", 0).set("uDepthTex", 1);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, src.colorTex);
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, sceneFb.depthTex);
            glActiveTexture(GL_TEXTURE0);

            glBindVertexArray(fullscreenVao);
            glDrawArrays(GL_TRIANGLES, 0, 3);
            glBindVertexArray(0);
            e.shader.unbind();

            if (dst != null) src = dst;
        }

        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    private void ensureInit() {
        if (fullscreenVao == 0) fullscreenVao = glGenVertexArrays();
    }

    private void ensureTargets(int w, int h) {
        if (sceneFb != null && sceneFb.width == w && sceneFb.height == h) return;
        closeTargets();
        sceneFb = new Framebuffer(w, h);
        depthCopy = Framebuffer.depthOnly(w, h);
    }

    private void closeTargets() {
        if (sceneFb != null) sceneFb.close();
        if (depthCopy != null) depthCopy.close();
        if (pingA != null) pingA.close();
        if (pingB != null) pingB.close();
        sceneFb = depthCopy = pingA = pingB = null;
    }

    @Override
    public void close() {
        closeTargets();
        if (fullscreenVao != 0) glDeleteVertexArrays(fullscreenVao);
        fullscreenVao = 0;
    }
}