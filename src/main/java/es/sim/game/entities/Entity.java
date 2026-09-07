package es.sim.game.entities;

import es.sim.util.Identifier;

import java.awt.*;

public abstract class Entity {
    private Point pos;
    private final Identifier ENTITY_ID;
    
    protected Entity(Identifier entityID) {
        this.ENTITY_ID = entityID;
    }

    public void tick() {
    }
}
