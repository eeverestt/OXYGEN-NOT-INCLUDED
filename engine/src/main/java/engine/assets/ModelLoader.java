package engine.assets;

import engine.gfx.Mesh;
import engine.util.Color;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class ModelLoader {
    private static final int FLAGS =
            aiProcess_Triangulate
                    | aiProcess_GenNormals
                    | aiProcess_JoinIdenticalVertices
                    | aiProcess_FlipUVs
                    | aiProcess_PreTransformVertices
                    | aiProcess_SortByPType;

    private ModelLoader() {}

    public static Model load(String path) {
        byte[] bytes = Resources.readBytes(path);
        ByteBuffer data = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        AIScene scene = null;
        try {
            scene = aiImportFileFromMemory(data, FLAGS, extension(path));
            if (scene == null || scene.mRootNode() == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0)
                throw new RuntimeException("Failed to load model " + path + ": " + aiGetErrorString());
            return build(scene, path);
        } finally {
            if (scene != null) aiReleaseImport(scene);
            MemoryUtil.memFree(data);
        }
    }

    private static Model build(AIScene scene, String path) {
        List<Model.Part> parts = new ArrayList<>();
        PointerBuffer meshes = scene.mMeshes();
        PointerBuffer materials = scene.mMaterials(); // may be null

        for (int i = 0; i < scene.mNumMeshes(); i++) {
            AIMesh m = AIMesh.create(meshes.get(i));
            if ((m.mPrimitiveTypes() & aiPrimitiveType_TRIANGLE) == 0) continue;
            Color color = baseColor(materials, scene.mNumMaterials(), m.mMaterialIndex());
            parts.add(new Model.Part(m.mName().dataString(), buildMesh(m), color));
        }
        if (parts.isEmpty()) throw new RuntimeException("Model has no triangle meshes: " + path);
        return new Model(path, parts);
    }

    private static Mesh buildMesh(AIMesh m) {
        int n = m.mNumVertices();
        AIVector3D.Buffer pos = m.mVertices();
        AIVector3D.Buffer nor = m.mNormals();
        AIVector3D.Buffer uv = m.mTextureCoords(0);
        AIColor4D.Buffer col = m.mColors(0);

        boolean hasColor = col != null;
        int stride = 8 + (hasColor ? 4 : 0);
        float[] v = new float[n * stride];

        for (int i = 0, o = 0; i < n; i++) {
            AIVector3D p = pos.get(i);
            v[o++] = p.x(); v[o++] = p.y(); v[o++] = p.z();

            if (nor != null) {
                AIVector3D nn = nor.get(i);
                v[o++] = nn.x(); v[o++] = nn.y(); v[o++] = nn.z();
            } else {
                v[o++] = 0; v[o++] = 1; v[o++] = 0;
            }

            if (uv != null) {
                AIVector3D t = uv.get(i);
                v[o++] = t.x(); v[o++] = t.y();
            } else {
                v[o++] = 0; v[o++] = 0;
            }

            if (hasColor) {
                AIColor4D c = col.get(i);
                v[o++] = c.r(); v[o++] = c.g(); v[o++] = c.b(); v[o++] = c.a();
            }
        }

        AIFace.Buffer faces = m.mFaces();
        int faceCount = m.mNumFaces();
        int tris = 0;
        for (int i = 0; i < faceCount; i++) if (faces.get(i).mNumIndices() == 3) tris++;

        int[] idx = new int[tris * 3];
        int j = 0;
        for (int i = 0; i < faceCount; i++) {
            AIFace f = faces.get(i);
            if (f.mNumIndices() != 3) continue;
            IntBuffer ib = f.mIndices();
            idx[j++] = ib.get(0);
            idx[j++] = ib.get(1);
            idx[j++] = ib.get(2);
        }

        int[] layout = hasColor ? new int[]{3, 3, 2, 4} : Mesh.STANDARD;
        return new Mesh(v, idx, layout);
    }

    private static Color baseColor(PointerBuffer materials, int materialCount, int index) {
        if (materials == null || index < 0 || index >= materialCount)
            return new Color(0xFFFFFF);

        AIMaterial mat = AIMaterial.create(materials.get(index));

        try (MemoryStack s = stackPush()) {
            AIColor4D col = AIColor4D.malloc(s);

            if (aiGetMaterialColor(mat, AI_MATKEY_COLOR_DIFFUSE, aiTextureType_NONE, 0, col) == aiReturn_SUCCESS)
                return new Color(
                        Math.round(col.r() * 255),
                        Math.round(col.g() * 255),
                        Math.round(col.b() * 255)
                );
        }

        return new Color(0xFFFFFF);
    }

    private static String extension(String path) {
        int dot = path.lastIndexOf('.');
        return dot < 0 ? "" : path.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}