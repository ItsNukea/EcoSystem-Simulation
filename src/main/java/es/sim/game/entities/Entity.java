package es.sim.game.entities;

import es.sim.util.Identifier;
import es.sim.game.board.Board;

import java.awt.*;

/// This class represents the super class of all living things on the {@link Board}, every entity should
/// extend this class to be able to be rendered and ticked
public abstract class Entity {
    protected Point pos;
    private final Identifier ENTITY_ID;
    
    protected Entity(Identifier entityID) {
        this.ENTITY_ID = entityID;
    }

    public abstract void tick();
    public abstract void render(Graphics2D graphics, Rectangle cellBounds);

    public Identifier getEntityID() {
        return ENTITY_ID;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }
    public void setPos(int x, int y) {
        setPos(new Point(x, y));
    }

    protected void move(int dx, int dy) {
        pos.translate(dx, dy);
    }
}

