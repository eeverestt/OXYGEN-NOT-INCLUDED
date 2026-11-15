package game;

public final class Waves {
    public static float height(float x, float z, float time) {
        float h = 0f;

        h += wave(x, z, 1.0f, 0.35f, 0.09f, 1.8f, 0.65f, time);
        h += wave(x, z, -0.4f, 1.0f, 0.15f, 1.1f, 0.85f, time);
        h += wave(x, z, 0.7f, -1.0f, 0.25f, 0.65f, 1.2f, time);
        h += wave(x, z, -1.0f, -0.2f, 0.38f, 0.3f, 1.7f, time);

        h += wave(x, z, 1.0f, 1.0f, 0.55f, 0.16f, 2.1f, time);
        h += wave(x, z, -0.8f, 0.6f, 0.75f, 0.09f, 2.5f, time);

        return h;
    }

    private static float wave(float x, float z, float dx, float dz, float frequency, float amplitude, float speed, float time) {
        float length = (float) Math.sqrt(dx * dx + dz * dz);
        float d = (x * dx + z * dz) / length;
        return (float) Math.sin(d * frequency + time * speed) * amplitude;
    }
}
