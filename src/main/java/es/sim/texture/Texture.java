package es.sim.texture;

import es.sim.*;
import es.sim.util.*;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import static es.sim.Main.LOGGER;

/** This class represents a Texture that can be drawed to the {@link Window}.<br>
  * This class is meant to make it easier to draw textures without having to read their .png files manually first.<br>
  * Together with {@link TextureManager}, this class also helps memory-usage, as a lot of textures loaded into memory
  * at once uses a lot of RAM, which we do NOT want in a memory shortage
**/
public class Texture {
    public static final Texture MISSING_TEXTURE =
            new Texture(Identifier.withDefaultNamespace("missing"));

    private final Identifier id;

    public Texture(Identifier id) {
        this.id = id;
    }

    public BufferedImage asImage() {
        BufferedImage image = TextureManager.getImage(id);

        if (image != null) {
            return image;
        }

        return loadImageAndGet();
    }

    private BufferedImage loadImageAndGet() {
        // Another Texture may have loaded it between the previous check
        // and this call, so check again.
        BufferedImage existing = TextureManager.getImage(id);

        if (existing != null) {
            return existing;
        }

        BufferedImage image;

        try {
            InputStream in = Texture.class.getResourceAsStream(getResourceName());

            if (in == null) {
                in = getMissingTextureInputStream();
                assert in != null;
            }

            image = ImageIO.read(in);
            in.close();

        } catch (IOException e) {
            image = MISSING_TEXTURE.asImage();
        }

        TextureManager.register(this, image);

        return image;
    }

    public long getImageByteSize() {
        BufferedImage image = TextureManager.getImage(id);
        long byteSize;

        if (image != null) {
            DataBuffer buffer = image.getRaster().getDataBuffer();
            long bytesPerElement =
                    DataBuffer.getDataTypeSize(buffer.getDataType()) / 8L;
            byteSize = buffer.getSize() * bytesPerElement;
        } else try {
            ImageInputStream in = ImageIO.createImageInputStream(new File(getResourceName()));
            if (in == null) {
                in = ImageIO.createImageInputStream(getMissingTextureInputStream());
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("No ImageReader found for: " + id.toString());
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
            throw new UncheckedIOException("Failed to read image size for: " + id.toString(), e);
        }

        return byteSize;
    }

    public Identifier getIdentifier() {
        return id;
    }

    private InputStream getMissingTextureInputStream() {
        return Texture.class.getResourceAsStream(
                "/textures/ess/missing.png"
        );
    }

    private String getResourceName() {
        return "/textures/" + id.getNamespace()
                + "/" + id.getPath() + ".png";
    }
}