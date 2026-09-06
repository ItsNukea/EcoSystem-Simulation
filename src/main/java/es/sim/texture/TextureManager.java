package es.sim.texture;

import java.awt.image.*;
import java.util.*;

public class TextureManager {
    public static final ArrayList<Texture> textures = new ArrayList<>();
    public static final long MAX_BYTE_BUFFER_SIZE = 512L * 1024 * 1024;    //I'd say about 512 MB
    public static long currentBufferSize = 0;

    public static void register(Texture tex) {
        long byteSize = tex.getImageByteSize();
        currentBufferSize += byteSize;
        while(currentBufferSize > MAX_BYTE_BUFFER_SIZE) {
            Texture first = textures.getFirst();
            long freedUpByteSize = first.getImageByteSize();
            first.freeUpMemory();
            textures.remove(first);
            currentBufferSize -= freedUpByteSize;
        }
        textures.add(tex);
    }
}
