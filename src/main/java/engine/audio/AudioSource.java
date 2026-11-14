package engine.audio;

import org.joml.Vector3f;

import static org.lwjgl.openal.AL10.*;

public final class AudioSource implements AutoCloseable {
    private final int id;

    public AudioSource() {
        id = alGenSources();
        rolloff(1f, 2f, 100f);
    }

    public AudioSource sound(Sound s) {
        stop();
        alSourcei(id, AL_BUFFER, s.buffer);
        return this;
    }

    public AudioSource position(float x, float y, float z) { alSource3f(id, AL_POSITION, x, y, z); return this; }
    public AudioSource position(Vector3f p) { return position(p.x, p.y, p.z); }
    public AudioSource gain(float g)        { alSourcef(id, AL_GAIN, g); return this; }
    public AudioSource pitch(float p)       { alSourcef(id, AL_PITCH, p); return this; }
    public AudioSource loop(boolean on)     { alSourcei(id, AL_LOOPING, on ? AL_TRUE : AL_FALSE); return this; }

    public AudioSource relative(boolean on) { alSourcei(id, AL_SOURCE_RELATIVE, on ? AL_TRUE : AL_FALSE); return this; }

    public AudioSource rolloff(float factor, float refDist, float maxDist) {
        alSourcef(id, AL_ROLLOFF_FACTOR, factor);
        alSourcef(id, AL_REFERENCE_DISTANCE, refDist);
        alSourcef(id, AL_MAX_DISTANCE, maxDist);
        return this;
    }

    public void play()  { alSourcePlay(id); }
    public void pause() { alSourcePause(id); }
    public void stop()  { alSourceStop(id); }

    public boolean isPlaying() { return alGetSourcei(id, AL_SOURCE_STATE) == AL_PLAYING; }

    @Override
    public void close() {
        stop();
        alDeleteSources(id);
    }
}
