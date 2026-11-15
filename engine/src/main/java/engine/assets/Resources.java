package engine.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class Resources {
    private Resources() {}

    public static byte[] readBytes(String path) {
        String p = path.startsWith("/") ? path.substring(1) : path;
        try (InputStream in = Resources.class.getClassLoader().getResourceAsStream(p)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
            return in.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    public static String readText(String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }
}
