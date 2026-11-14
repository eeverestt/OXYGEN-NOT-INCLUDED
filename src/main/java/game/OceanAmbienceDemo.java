package game;

import engine.audio.AudioSource;
import engine.audio.Sound;
import org.joml.Vector3f;

public class OceanAmbienceDemo implements AutoCloseable {
    public static final String PATH = "sounds/ocean.wav";

    private static final float BASE_GAIN = 0.25f;
    private static final float SWELL = 0.25f;
    private static final float REF_DIST = 6f;
    private static final float MAX_DIST = 80f;

    private final Sound sound;
    private final AudioSource source;
    private float restY = Float.NaN;

    public OceanAmbienceDemo() {
        Sound loaded = null;
        try {
            loaded = Sound.loadMono(PATH);
        } catch (RuntimeException e) {
            System.err.println("[OceanAmbience] " + e.getMessage());
        }
        sound = loaded;
        if (sound == null) {
            source = null;
            return;
        }

        source = new AudioSource().sound(sound).loop(true).gain(BASE_GAIN).rolloff(1f, REF_DIST, MAX_DIST);
        source.play();
    }

    public void update(Raft raft) {
        if (source == null) return;
        Vector3f p = raft.deck.transform.position;
        source.position(p);

        if (Float.isNaN(restY)) restY = p.y;
        restY += (p.y - restY) * 0.01f;
        float lift = Math.max(-1f, Math.min(1f, p.y - restY));
        source.gain(BASE_GAIN * (1f + SWELL * lift));
    }

    @Override
    public void close() {
        if (source != null) source.close();
        if (sound != null) sound.close();
    }
}
