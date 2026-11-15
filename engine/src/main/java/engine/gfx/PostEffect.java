
package engine.gfx;

public final class PostEffect implements AutoCloseable {
    public static final String FULLSCREEN_VERT = "shaders/fullscreen.vert";

    @FunctionalInterface
    public interface Setup {
        void apply(Shader shader, FrameContext frame);
    }

    public final Shader shader;
    public boolean enabled = true;
    private final Setup setup;

    public PostEffect(Shader shader, Setup setup) {
        this.shader = shader;
        this.setup = setup;
    }

    public static PostEffect fromFragment(String fragPath, Setup setup) {
        return new PostEffect(Shader.load(FULLSCREEN_VERT, fragPath), setup);
    }

    void configure(FrameContext frame) { setup.apply(shader, frame); }

    @Override
    public void close() { shader.close(); }
}
 