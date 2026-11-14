package engine.audio;

import engine.scene.Camera;
import org.joml.Vector3f;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALCCapabilities;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class Audio implements AutoCloseable {
    private static final int ONE_SHOT_SOURCES = 16;

    private final long device;
    private final long context;
    private final AudioSource[] pool = new AudioSource[ONE_SHOT_SOURCES];
    private int nextSource;

    private final float[] orientation = new float[6];

    public Audio() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) throw new IllegalStateException("Failed to open the default OpenAL device");

        ALCCapabilities alcCaps = ALC.createCapabilities(device);
        context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            alcCloseDevice(device);
            throw new IllegalStateException("Failed to create an OpenAL context");
        }
        alcMakeContextCurrent(context);
        AL.createCapabilities(alcCaps);

        alDistanceModel(AL_INVERSE_DISTANCE_CLAMPED);

        for (int i = 0; i < pool.length; i++) pool[i] = new AudioSource();
    }

    public void listenTo(Camera cam) {
        Vector3f f = cam.forward();
        alListener3f(AL_POSITION, cam.position.x, cam.position.y, cam.position.z);
        orientation[0] = f.x; orientation[1] = f.y; orientation[2] = f.z;
        orientation[3] = 0f;  orientation[4] = 1f;  orientation[5] = 0f;
        alListenerfv(AL_ORIENTATION, orientation);
    }

    public void masterVolume(float gain) { alListenerf(AL_GAIN, gain); }

    public AudioSource play(Sound sound, float x, float y, float z, float gain) { return play(sound, false, x, y, z, gain); }
    public AudioSource play(Sound sound, Vector3f pos, float gain)            { return play(sound, false, pos.x, pos.y, pos.z, gain); }
    public AudioSource play2D(Sound sound, float gain)                        { return play(sound, true, 0f, 0f, 0f, gain); }

    private AudioSource play(Sound sound, boolean relative, float x, float y, float z, float gain) {
        int i = 0;
        while (i < pool.length && pool[(nextSource + i) % pool.length].isPlaying()) i++;
        if (i == pool.length) i = 0;
        AudioSource s = pool[(nextSource + i) % pool.length];
        nextSource = (nextSource + i + 1) % pool.length;
        s.relative(relative).position(x, y, z).gain(gain).pitch(1f).loop(false).sound(sound).play();
        return s;
    }

    @Override
    public void close() {
        for (AudioSource s : pool) s.close();
        alcMakeContextCurrent(NULL);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }
}
