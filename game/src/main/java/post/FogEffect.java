package post;

import engine.gfx.FrameContext;
import engine.gfx.PostEffect;
import engine.gfx.Shader;
import engine.util.Color;
import org.joml.Vector3f;

public final class FogEffect implements AutoCloseable {
    public final Vector3f color = new Color(0x696568).toNormalisedVector();
    public float density = 0.12f;
    public float heightFalloff = 0.15f;
    public float baseHeight = 2.0f;
    public float noiseScale = 0.05f;
    public float maxDistance = 200f;

    private final PostEffect effect;

    public FogEffect() {
        effect = PostEffect.fromFragment("shaders/volumetric_fog.frag", this::setUniforms);
    }

    public FogEffect setColor(int hex) {
        color.set(new Color(hex).toNormalisedVector());
        return this;
    }

    public PostEffect effect() { return effect; }

    private void setUniforms(Shader s, FrameContext f) {
        s.set("uInvViewProj", f.invViewProj)
                .set("uCameraPos", f.scene.camera.position)
                .set("uLightDir", f.scene.lightDir)
                .set("uFogColor", color)
                .set("uFogDensity", density)
                .set("uFogHeightFalloff", heightFalloff)
                .set("uFogBaseHeight", baseHeight)
                .set("uNoiseScale", noiseScale)
                .set("uMaxDistance", maxDistance)
                .set("uTime", f.time);
    }

    @Override
    public void close() { effect.close(); }
}