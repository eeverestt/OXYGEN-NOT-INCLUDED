package engine.audio;

import org.junit.jupiter.api.Test;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class SoundDecodeTest {
    private static byte[] wav(byte[] frames, int bits, int channels, boolean signed) throws Exception {
        AudioFormat f = new AudioFormat(22050, bits, channels, signed, false);
        var in = new AudioInputStream(new ByteArrayInputStream(frames), f, frames.length / f.getFrameSize());
        var out = new ByteArrayOutputStream();
        AudioSystem.write(in, AudioFileFormat.Type.WAVE, out);
        return out.toByteArray();
    }

    @Test
    void decodes16BitMonoWav() throws Exception {
        Sound.Pcm pcm = Sound.decode(wav(new byte[]{(byte) 0xE8, 0x03, 0x18, (byte) 0xFC}, 16, 1, true));
        assertEquals(1, pcm.channels());
        assertEquals(22050, pcm.sampleRate());
        assertArrayEquals(new short[]{1000, -1000}, pcm.samples());
    }

    @Test
    void decodes8BitUnsignedWavTo16Bit() throws Exception {
        Sound.Pcm pcm = Sound.decode(wav(new byte[]{(byte) 128, (byte) 255, 0}, 8, 1, false));
        assertClose(new short[]{0, 32767, -32768}, pcm.samples(), 256);
    }

    @Test
    void decodes24BitStereoWav() throws Exception {
        Sound.Pcm pcm = Sound.decode(wav(new byte[]{0x56, 0x34, 0x12, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, 24, 2, true));
        assertEquals(2, pcm.channels());
        assertClose(new short[]{0x1234, -1}, pcm.samples(), 1);
    }

    private static void assertClose(short[] expected, short[] actual, int tolerance) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
            assertTrue(Math.abs(expected[i] - actual[i]) <= tolerance, "sample " + i + ": expected ~" + expected[i] + " but was " + actual[i]);
    }

    @Test
    void downmixesStereoToMono() {
        Sound.Pcm stereo = new Sound.Pcm(new short[]{1000, 3000, 32767, 32767, -32768, -32768, 100, -100}, 2, 44100);
        Sound.Pcm mono = Sound.toMono(stereo);
        assertEquals(1, mono.channels());
        assertEquals(44100, mono.sampleRate());
        assertArrayEquals(new short[]{2000, 32767, -32768, 0}, mono.samples());
    }

    @Test
    void namesUnsupportedFormats() {
        byte[] flac = {'f', 'L', 'a', 'C', 0, 0, 0, 0};
        var e = assertThrows(RuntimeException.class, () -> Sound.decode(flac));
        assertTrue(e.getMessage().contains("FLAC"), e.getMessage());
        assertTrue(e.getMessage().contains("Ogg Vorbis or WAV"), e.getMessage());
    }
}
