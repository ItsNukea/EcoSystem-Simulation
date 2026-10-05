package es.sim.texture;

import es.sim.Window;
import es.sim.util.*;

import javax.imageio.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;

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
        BufferedImage image = TextureManager.getImage(id);

        if (image != null) {
            return image;
        }

        //The image doesn't exist, read it from the file
        try {
            InputStream in = Texture.class.getResourceAsStream(getResourceName());

            if (in == null) {
                image = MISSING_TEXTURE.asImage();
                return image;
            }

            image = ImageIO.read(in);
            in.close();

        } catch (IOException e) {
            image = MISSING_TEXTURE.asImage();
        }

        TextureManager.register(this, image);

        return image;
    }

    public Identifier getIdentifier() {
        return id;
    }

    public Dimension getDimension() {
        BufferedImage image = TextureManager.getImage(id);
        if(image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        } else {
            try {
                InputStream in = Texture.class.getResourceAsStream(getResourceName());
                if(in == null) {
                    in = getMissingTextureInputStream();
                }
                DataInputStream din = new DataInputStream(in);

                byte[] signature = new byte[8];
                din.readFully(signature);

                din.readInt();
                int chunkType = din.readInt();

                if (chunkType != 0x49484452) { // "IHDR"
                    throw new IOException("Invalid PNG");
                }

                int width = din.readInt();
                int height = din.readInt();

                in.close();
                din.close();

                return new Dimension(width, height);
            } catch(IOException e) {
                throw new UncheckedIOException("Could not retrieve image dimension", e);
            }
        }
    }

    public int getWidth() {
        return getDimension().width;
    }

    public int getHeight() {
        return getDimension().height;
    }

    private InputStream getMissingTextureInputStream() {
        return Texture.class.getResourceAsStream("/textures/ess/missing.png");
    }

    private String getResourceName() {
        return "/textures/" + id.getNamespace() + "/" + id.getPath() + ".png";
    }
}