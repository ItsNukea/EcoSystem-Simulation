package es.sim.gui;

import es.sim.*;
import es.sim.texture.*;
import es.sim.util.*;

import java.awt.*;

public class TitleScreen extends Screen {
    public TitleScreen() {
        super(null);
    }

    @Override
    public void render(Graphics2D g) {
        Texture texture = Texture.MISSING_TEXTURE;
        g.drawImage(texture.asImage(), 0, 0, 100, 100, this);
    }
}
