package game.init;

import engine.gfx.Mesh;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class Ocean {
    public float amp1 = 0.26f, amp2 = 0.18f, amp3 = 0.13f;
    public float freq1 = 0.72f, freq2 = 0.51f, freq3 = 0.33f;
    public float speed1 = 1.35f, speed2 = 0.94f, speed3 = 0.61f;
    public float heightStep = 0.10f;
    public float baseLevel = 2f;
    public float levelScale = 3.5f;

    public int minHeight = 0, maxHeight = 7;

    public boolean parallel = true;

    private final int resolution;
    private final float size;
    private final float cellSize;

    private final int[] heights;
    private final boolean[] merged;
    private final int[] keys;
    private final VoxelMeshBuilder builder = new VoxelMeshBuilder();

    private final float[] sinX, cosX, sinZ, cosZ, sinS, cosS;

    private static final ConcurrentHashMap<CacheKey, Ocean> CACHE = new ConcurrentHashMap<>();
    private record CacheKey(int resolution, float size) {}

    public Ocean(int resolution, float size) {
        this.resolution = resolution;
        this.size       = size;
        this.cellSize   = size / resolution;

        this.heights = new int[resolution * resolution];
        this.merged  = new boolean[resolution * resolution];
        this.keys    = new int[resolution];

        int sLen = 2 * resolution - 1;
        this.sinX = new float[resolution]; this.cosX = new float[resolution];
        this.sinZ = new float[resolution]; this.cosZ = new float[resolution];
        this.sinS = new float[sLen];       this.cosS = new float[sLen];

        retune();
    }

    public void retune() {
        for (int i = 0; i < resolution; i++) {
            float w = (i + 0.5f) * cellSize - size * 0.5f;
            sinX[i] = (float) Math.sin(w * freq1);
            cosX[i] = (float) Math.cos(w * freq1);
            sinZ[i] = (float) Math.sin(w * freq2);
            cosZ[i] = (float) Math.cos(w * freq2);
        }
        int sLen = 2 * resolution - 1;
        for (int s = 0; s < sLen; s++) {
            float w = (s + 1) * cellSize - size;
            sinS[s] = (float) Math.sin(w * freq3);
            cosS[s] = (float) Math.cos(w * freq3);
        }
    }

    public static Mesh voxelOcean(int resolution, float size, float time) {
        Ocean m = CACHE.computeIfAbsent(new CacheKey(resolution, size),
                k -> new Ocean(k.resolution, k.size));
        return m.render(time);
    }

    public Mesh render(float time) {
        rebuildHeightmap(time);
        return buildMesh();
    }

    private void rebuildHeightmap(float time) {
        final float sT1 = (float) Math.sin(time * speed1), cT1 = (float) Math.cos(time * speed1);
        final float sT2 = (float) Math.sin(time * speed2), cT2 = (float) Math.cos(time * speed2);
        final float sT3 = (float) Math.sin(time * speed3), cT3 = (float) Math.cos(time * speed3);

        if (parallel && resolution * resolution >= 16384) {
            IntStream.range(0, resolution).parallel()
                    .forEach(x -> computeRow(x, sT1, cT1, sT2, cT2, sT3, cT3));
        } else {
            for (int x = 0; x < resolution; x++)
                computeRow(x, sT1, cT1, sT2, cT2, sT3, cT3);
        }
    }

    private void computeRow(int x,
                            float sT1, float cT1,
                            float sT2, float cT2,
                            float sT3, float cT3) {
        final int row = x * resolution;
        final float w1x = sinX[x] * cT1 + cosX[x] * sT1;

        int s = x;
        for (int z = 0; z < resolution; z++, s++) {
            float w2 = sinZ[z] * cT2 - cosZ[z] * sT2;
            float w3 = sinS[s] * cT3 + cosS[s] * sT3;

            float wave = w1x * amp1 + w2 * amp2 + w3 * amp3;
            int h = (int) Math.floor(baseLevel + (wave + 1.0f) * levelScale);
            if (h < minHeight) h = minHeight;
            if (h > maxHeight) h = maxHeight;
            heights[row + z] = h;
        }
    }

    private Mesh buildMesh() {
        builder.reset();
        Arrays.fill(merged, false);

        buildTops();
        buildXSides(false);
        buildXSides(true);
        buildZSides(false);
        buildZSides(true);

        return new Mesh(builder.vertices(), builder.indices());
    }

    private void buildTops() {
        for (int x = 0; x < resolution; x++) {
            for (int z = 0; z < resolution; z++) {
                if (merged[x * resolution + z]) continue;

                int level = heights[x * resolution + z];

                int width = 1;
                while (x + width < resolution
                        && !merged[(x + width) * resolution + z]
                        && heights[(x + width) * resolution + z] == level) {
                    width++;
                }

                int depth = 1;
                boolean valid = true;
                while (z + depth < resolution && valid) {
                    for (int i = 0; i < width; i++) {
                        if (merged[(x + i) * resolution + z + depth]
                                || heights[(x + i) * resolution + z + depth] != level) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) depth++;
                }

                for (int i = 0; i < width; i++)
                    for (int j = 0; j < depth; j++)
                        merged[(x + i) * resolution + z + j] = true;

                float x0 = x * cellSize - size * 0.5f;
                float x1 = (x + width) * cellSize - size * 0.5f;
                float z0 = z * cellSize - size * 0.5f;
                float z1 = (z + depth) * cellSize - size * 0.5f;
                float y  = level * heightStep;

                builder.quad(x0, y, z0, x0, y, z1, x1, y, z1, x1, y, z0,
                        0, 1, 0, level);
            }
        }
    }

    private void buildXSides(boolean positive) {
        for (int x = 0; x < resolution; x++) {
            Arrays.fill(keys, -1);
            for (int z = 0; z < resolution; z++) {
                int height = heights[x * resolution + z];
                int nb = positive
                        ? (x + 1 < resolution ? heights[(x + 1) * resolution + z] : 0)
                        : (x > 0             ? heights[(x - 1) * resolution + z] : 0);
                if (height > nb) keys[z] = (nb << 8) | height;
            }

            for (int z = 0; z < resolution; ) {
                if (keys[z] == -1) { z++; continue; }
                int key = keys[z];
                int end = z + 1;
                while (end < resolution && keys[end] == key) end++;

                int bottom = key >>> 8;
                int top    = key & 255;

                float px = (positive ? (x + 1) : x) * cellSize - size * 0.5f;
                float z0 = z * cellSize - size * 0.5f;
                float z1 = end * cellSize - size * 0.5f;
                float y0 = bottom * heightStep;
                float y1 = top * heightStep;

                if (positive) {
                    builder.quad(px, y0, z1, px, y0, z0, px, y1, z0, px, y1, z1,
                            1, 0, 0, 8 + top);
                } else {
                    builder.quad(px, y0, z0, px, y0, z1, px, y1, z1, px, y1, z0,
                            -1, 0, 0, 8 + top);
                }
                z = end;
            }
        }
    }

    private void buildZSides(boolean positive) {
        for (int z = 0; z < resolution; z++) {
            Arrays.fill(keys, -1);
            for (int x = 0; x < resolution; x++) {
                int height = heights[x * resolution + z];
                int nb = positive
                        ? (z + 1 < resolution ? heights[x * resolution + z + 1] : 0)
                        : (z > 0             ? heights[x * resolution + z - 1] : 0);
                if (height > nb) keys[x] = (nb << 8) | height;
            }

            for (int x = 0; x < resolution; ) {
                if (keys[x] == -1) { x++; continue; }
                int key = keys[x];
                int end = x + 1;
                while (end < resolution && keys[end] == key) end++;

                int bottom = key >>> 8;
                int top    = key & 255;

                float pz = (positive ? (z + 1) : z) * cellSize - size * 0.5f;
                float x0 = x * cellSize - size * 0.5f;
                float x1 = end * cellSize - size * 0.5f;
                float y0 = bottom * heightStep;
                float y1 = top * heightStep;

                if (positive) {
                    builder.quad(x0, y0, pz, x1, y0, pz, x1, y1, pz, x0, y1, pz,
                            0, 0, 1, 8 + top);
                } else {
                    builder.quad(x1, y0, pz, x0, y0, pz, x0, y1, pz, x1, y1, pz,
                            0, 0, -1, 8 + top);
                }
                x = end;
            }
        }
    }

    private static class VoxelMeshBuilder {
        private float[] vertices = new float[4096];
        private int[]   indices  = new int[4096];
        private int vertexCount, indexCount;

        void reset() { vertexCount = 0; indexCount = 0; }

        void quad(float x0, float y0, float z0,
                  float x1, float y1, float z1,
                  float x2, float y2, float z2,
                  float x3, float y3, float z3,
                  float nx, float ny, float nz,
                  int palette) {
            ensureVertices(32);
            ensureIndices(6);

            float uv = palette;
            vertex(x0, y0, z0, nx, ny, nz, uv, 0);
            vertex(x1, y1, z1, nx, ny, nz, uv, 0);
            vertex(x2, y2, z2, nx, ny, nz, uv, 0);
            vertex(x3, y3, z3, nx, ny, nz, uv, 0);

            int base = vertexCount / 8 - 4;
            indices[indexCount++] = base;
            indices[indexCount++] = base + 1;
            indices[indexCount++] = base + 2;
            indices[indexCount++] = base;
            indices[indexCount++] = base + 2;
            indices[indexCount++] = base + 3;
        }

        private void vertex(float x, float y, float z,
                            float nx, float ny, float nz,
                            float u, float v) {
            vertices[vertexCount++] = x;
            vertices[vertexCount++] = y;
            vertices[vertexCount++] = z;
            vertices[vertexCount++] = nx;
            vertices[vertexCount++] = ny;
            vertices[vertexCount++] = nz;
            vertices[vertexCount++] = u;
            vertices[vertexCount++] = v;
        }

        private void ensureVertices(int n) {
            if (vertexCount + n > vertices.length)
                vertices = Arrays.copyOf(vertices,
                        Math.max(vertices.length * 2, vertexCount + n));
        }
        private void ensureIndices(int n) {
            if (indexCount + n > indices.length)
                indices = Arrays.copyOf(indices,
                        Math.max(indices.length * 2, indexCount + n));
        }

        float[] vertices() {
            return Arrays.copyOf(vertices, vertexCount);
        }

        int[] indices() {
            return Arrays.copyOf(indices,  indexCount);
        }
    }
}