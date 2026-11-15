package engine.util;

import org.joml.Vector3f;
import org.joml.Vector3i;

public record Color(int r, int g, int b) {
    public Color(int hex) {
        this(
                (hex >> 16) & 0xFF,
                (hex >> 8) & 0xFF,
                hex & 0xFF
        );
    }

    public Vector3i toVector() {
        return new Vector3i(r, g, b);
    }

    public Vector3f toNormalisedVector() {
        return new Vector3f(
                r / 255.0f,
                g / 255.0f,
                b / 255.0f
        );
    }
}