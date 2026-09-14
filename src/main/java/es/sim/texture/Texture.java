package es.sim.texture;

import es.sim.util.*;
import es.sim.Window;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import static es.sim.Main.*;

/** This class represents a Texture that can be drawed to the {@link Window}.<br>
  * This class is meant to make it easier to draw textures without having to read their .png files manually first.<br>
  * Together with {@link TextureManager}, this class also helps memory-usage, as a lot of textures loaded into memory
  * at once uses a lot of RAM, which we do NOT want in a memory shortage
**/
public class Texture {
    public static final Texture MISSING_TEXTURE = new Texture(Identifier.withDefaultNamespace("missing"));

    private final Identifier id;
    private boolean loaded = false;
    private BufferedImage image = null;

    public Texture(Identifier id) {
        this.id = id;
    }

    public BufferedImage asImage() {
        if(loaded) {
            return image;
        } else {
            return loadImageAndGet();
        }
    }

    private BufferedImage loadImageAndGet() {
        try {
            InputStream in = Texture.class.getResourceAsStream(getResourceName());
            if(in == null) {
                in = getMissingTextureInputStream();
                assert in != null;
            }

            image = ImageIO.read(in);
            in.close();

            loaded = true;
        } catch (IOException e) {
            image = MISSING_TEXTURE.asImage();
            loaded = true;
        }
        TextureManager.register(this);
        return image;
    }

    public void freeUpMemory() {
        image = null;
        loaded = false;
    }

    public long getImageByteSize() {
        long byteSize;
        if (loaded) {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            long bytesPerElement = DataBuffer.getDataTypeSize(buffer.getDataType()) / 8L;
            byteSize = (long) buffer.getSize() * bytesPerElement;
        } else try {
            ImageInputStream in = ImageIO.createImageInputStream(new File(getResourceName()));
            if (in == null) {
                in = ImageIO.createImageInputStream(getMissingTextureInputStream());
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("No ImageReader found for: " + id.asString());
            }

            ImageReader reader = readers.next();
            reader.setInput(in);

            int width = reader.getWidth(0);
            int height = reader.getHeight(0);

            ImageTypeSpecifier rawType = reader.getRawImageType(0);
            int bitsPerPixel = rawType.getColorModel().getPixelSize();

            byteSize = (long) width * height * (bitsPerPixel / 8L);

            reader.dispose();
            in.close();
        } catch (IOException e) {
            LOGGER.error("Failed to get image size from a texture", e);
            throw new UncheckedIOException("Failed to read image size for: " + id.asString(), e);
        }
        return byteSize;
    }

    private InputStream getMissingTextureInputStream() {
        return Texture.class.getResourceAsStream("/textures/ess/missing.png");
    }

    private String getResourceName() {
        return "/textures/" + id.getNamespace() + "/" + id.getPath() + ".png";
    }
}
