package engine.gfx;

import static org.lwjgl.opengl.GL33C.*;

public final class Framebuffer implements AutoCloseable {
    public final int fbo, colorTex, depthTex, width, height;

    public Framebuffer(int w, int h) { this(w, h, true, true); }

    public static Framebuffer colorOnly(int w, int h) { return new Framebuffer(w, h, true, false); }
    public static Framebuffer depthOnly(int w, int h) { return new Framebuffer(w, h, false, true); }

    private Framebuffer(int w, int h, boolean withColor, boolean withDepth) {
        if (w <= 0 || h <= 0)
            throw new IllegalArgumentException("Framebuffer size must be positive, got " + w + "x" + h);
        if (!withColor && !withDepth)
            throw new IllegalArgumentException("Framebuffer needs at least one attachment");

        width = w;
        height = h;

        fbo = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);

        int c = 0;
        if (withColor) {
            c = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, c);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, w, h, 0, GL_RGBA, GL_FLOAT, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, c, 0);
        } else {
            glDrawBuffer(GL_NONE);
            glReadBuffer(GL_NONE);
        }
        colorTex = c;

        int d = 0;
        if (withDepth) {
            d = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, d);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, w, h, 0, GL_DEPTH_COMPONENT, GL_FLOAT, 0);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, d, 0);
        }
        depthTex = d;

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);

        if (status != GL_FRAMEBUFFER_COMPLETE) {
            close();
            throw new IllegalStateException("Framebuffer incomplete (status 0x" + Integer.toHexString(status) + ")");
        }
    }

    public void bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glViewport(0, 0, width, height);
    }

    public void unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void copyDepthTo(Framebuffer dst) {
        if (depthTex == 0 || dst.depthTex == 0)
            throw new IllegalStateException("Both framebuffers need a depth attachment");
        if (width != dst.width || height != dst.height)
            throw new IllegalArgumentException("Framebuffer sizes differ");
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, dst.fbo);
        glBlitFramebuffer(0, 0, width, height, 0, 0, dst.width, dst.height, GL_DEPTH_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public void blitColorToScreen() {
        if (colorTex == 0) throw new IllegalStateException("No colour attachment");
        glBindFramebuffer(GL_READ_FRAMEBUFFER, fbo);
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL_COLOR_BUFFER_BIT, GL_NEAREST);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public void close() {
        glDeleteFramebuffers(fbo);
        if (colorTex != 0) glDeleteTextures(colorTex);
        if (depthTex != 0) glDeleteTextures(depthTex);
    }
}