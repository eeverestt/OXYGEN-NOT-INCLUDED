package engine.audio;

import engine.assets.Resources;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.stb_vorbis_decode_memory;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Sound implements AutoCloseable {
    final int buffer;

    record Pcm(short[] samples, int channels, int sampleRate) {}

    private Sound(Pcm pcm) {
        buffer = alGenBuffers();
        alBufferData(buffer, pcm.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm.samples(), pcm.sampleRate());
    }

    public static Sound load(String path)     { return load(path, false); }
    public static Sound loadMono(String path) { return load(path, true); }

    private static Sound load(String path, boolean mono) {
        Pcm pcm;
        try {
            pcm = decode(Resources.readBytes(path));
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to load sound " + path + ": " + e.getMessage(), e);
        }
        return new Sound(mono ? toMono(pcm) : pcm);
    }

    static Pcm toMono(Pcm pcm) {
        if (pcm.channels() == 1) return pcm;
        short[] in = pcm.samples();
        short[] out = new short[in.length / 2];
        for (int i = 0; i < out.length; i++) out[i] = (short) ((in[2 * i] + in[2 * i + 1]) >> 1);
        return new Pcm(out, 1, pcm.sampleRate());
    }

    static Pcm decode(byte[] bytes) {
        Pcm pcm = startsWith(bytes, "OggS") ? decodeVorbis(bytes) : decodeWithJavaSound(bytes);
        if (pcm.channels() != 1 && pcm.channels() != 2)
            throw new RuntimeException(pcm.channels() + " channels; only mono or stereo is supported");
        return pcm;
    }

    private static Pcm decodeVorbis(byte[] bytes) {
        ByteBuffer raw = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        try (MemoryStack s = stackPush()) {
            IntBuffer channels = s.mallocInt(1), rate = s.mallocInt(1);
            ShortBuffer pcm = stb_vorbis_decode_memory(raw, channels, rate);
            if (pcm == null) throw unsupported(bytes);
            try {
                short[] out = new short[pcm.remaining()];
                pcm.get(out);
                return new Pcm(out, channels.get(0), rate.get(0));
            } finally {
                LibCStdlib.free(pcm);
            }
        } finally {
            MemoryUtil.memFree(raw);
        }
    }

    private static Pcm decodeWithJavaSound(byte[] bytes) {
        try (AudioInputStream src = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes))) {
            AudioFormat f = src.getFormat();
            AudioFormat pcm16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16,
                    f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
            byte[] data;
            try (AudioInputStream conv = AudioSystem.getAudioInputStream(pcm16, src)) {
                data = conv.readAllBytes();
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("can't convert " + f + " to 16-bit PCM");
            }
            short[] out = new short[data.length / 2];
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(out);
            return new Pcm(out, f.getChannels(), Math.round(f.getSampleRate()));
        } catch (UnsupportedAudioFileException e) {
            throw unsupported(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static RuntimeException unsupported(byte[] d) {
        return new RuntimeException("unsupported format" + formatHint(d) + " - export it as Ogg Vorbis or WAV");
    }

    private static String formatHint(byte[] d) {
        String head = new String(d, 0, Math.min(d.length, 64), java.nio.charset.StandardCharsets.ISO_8859_1);
        if (head.contains("FLAC") || head.startsWith("fLaC")) return " (FLAC)";
        if (head.contains("OpusHead")) return " (Opus)";
        if (head.startsWith("ID3") || (d.length > 1 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xE0) == 0xE0)) return " (MP3)";
        return "";
    }

    private static boolean startsWith(byte[] d, String magic) {
        if (magic.length() > d.length) return false;
        for (int i = 0; i < magic.length(); i++) if (d[i] != (byte) magic.charAt(i)) return false;
        return true;
    }

    @Override
    public void close() { alDeleteBuffers(buffer); }
}
