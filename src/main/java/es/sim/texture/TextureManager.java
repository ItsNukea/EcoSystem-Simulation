package es.sim.texture;

import es.sim.util.*;

import java.awt.image.*;
import java.util.*;

///Manages textures and makes sure all in-memory loaded textures do not exceed a size of 512MB
public class TextureManager {
    public static final ArrayDeque<Texture> textures = new ArrayDeque<>();

    private static final HashMap<Identifier, BufferedImage> imageBuffer =
            new HashMap<>();

    public static final long MAX_BYTE_BUFFER_SIZE = 512L * 1024 * 1024;
    public static long currentBufferSize = 0;

    public static BufferedImage getImage(Identifier id) {
        return imageBuffer.get(id);
    }

    public static void register(Texture tex, BufferedImage image) {
        Identifier id = tex.getIdentifier();

        // Already loaded by another Texture.
        if (imageBuffer.containsKey(id)) {
            return;
        }

        imageBuffer.put(id, image);
        textures.add(tex);

        currentBufferSize += getImageByteSize(image);

        while (currentBufferSize > MAX_BYTE_BUFFER_SIZE) {
            Texture first = textures.removeFirst();

            Identifier firstId = first.getIdentifier();
            BufferedImage firstImage = imageBuffer.remove(firstId);

            currentBufferSize -= getImageByteSize(firstImage);
        }
    }

    private static long getImageByteSize(BufferedImage image) {
        DataBuffer buffer = image.getRaster().getDataBuffer();

        long bytesPerElement =
                DataBuffer.getDataTypeSize(buffer.getDataType()) / 8L;

        return (long) buffer.getSize() * bytesPerElement;
    }
}