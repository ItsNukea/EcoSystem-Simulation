package es.sim.game.entities;

import es.sim.game.*;
import es.sim.texture.*;
import es.sim.util.*;

import java.awt.*;

import static es.sim.game.EntityState.*;

public class Deer extends Entity {
    private final int VIEW_DISTANCE = 8;
    private Point currentTarget;
    private EntityState ENTITY_STATE = WANDERING;

    public Deer() {
        super(Identifier.of("entity:deer"));
    }

    @Override
    public void tick() {
        if(currentTarget == null && ENTITY_STATE == WANDERING) {
            //findRandomTargetWithinViewDistance()
        }
    }

    @Override
    public void render(Graphics2D graphics, Rectangle cellBounds) {
        Texture sprite = new Texture(getEntityID());
        graphics.drawImage(
                sprite.asImage(),
                cellBounds.x,
                cellBounds.y,
                cellBounds.width,
                cellBounds.height,
                null
        );
    }
}
