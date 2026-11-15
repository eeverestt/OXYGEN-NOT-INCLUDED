package engine.gfx;

import engine.assets.Resources;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class Texture implements AutoCloseable {
    private final int id;

    private Texture(int w, int h, ByteBuffer rgba, boolean pixelArt) {
        id = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, id);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, pixelArt ? GL_NEAREST_MIPMAP_NEAREST : GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, pixelArt ? GL_NEAREST : GL_LINEAR);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, w, h, 0, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        glGenerateMipmap(GL_TEXTURE_2D);
    }

    public static Texture load(String path) { return load(path, false); }

    public static Texture load(String path, boolean pixelArt) {
        byte[] bytes = Resources.readBytes(path);
        ByteBuffer raw = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        try (MemoryStack s = stackPush()) {
            IntBuffer w = s.mallocInt(1), h = s.mallocInt(1), c = s.mallocInt(1);
            STBImage.stbi_set_flip_vertically_on_load(true);
            ByteBuffer pixels = STBImage.stbi_load_from_memory(raw, w, h, c, 4);
            if (pixels == null) throw new RuntimeException("Failed to load " + path + ": " + STBImage.stbi_failure_reason());
            try {
                return new Texture(w.get(0), h.get(0), pixels, pixelArt);
            } finally {
                STBImage.stbi_image_free(pixels);
            }
        } finally {
            MemoryUtil.memFree(raw);
        }
    }

    public static Texture fromRGBA(int w, int h, byte[] rgba, boolean pixelArt) {
        ByteBuffer buf = MemoryUtil.memAlloc(rgba.length).put(rgba).flip();
        try {
            return new Texture(w, h, buf, pixelArt);
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    public static Texture solid(int r, int g, int b) {
        return fromRGBA(1, 1, new byte[]{(byte) r, (byte) g, (byte) b, (byte) 255}, true);
    }

    public static Texture checker(int cells, int cellPx, int[] colorA, int[] colorB) {
        int size = cells * cellPx;
        byte[] px = new byte[size * size * 4];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int[] c = (((x / cellPx) + (y / cellPx)) % 2 == 0) ? colorA : colorB;
                int i = (y * size + x) * 4;
                px[i] = (byte) c[0]; px[i + 1] = (byte) c[1]; px[i + 2] = (byte) c[2]; px[i + 3] = (byte) 255;
            }
        }
        return fromRGBA(size, size, px, true);
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, id);
    }

    @Override
    public void close() { glDeleteTextures(id); }
}
