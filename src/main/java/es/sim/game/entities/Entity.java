package es.sim.game.entities;

import es.sim.*;
import es.sim.exceptions.*;
import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.util.*;

import java.awt.*;

/// This class represents the super class of all living things on the {@link Board}, every entity should
/// extend this class to be able to be rendered and ticked
public abstract class Entity {
    protected int VIEW_DISTANCE;

    protected Point pos;
    private final Identifier ENTITY_ID;
    protected final Board board;
    
    protected Entity(Identifier entityID) {
        this.ENTITY_ID = entityID;
        this.board = Main.board;
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

    protected void move(Direction d) {
        move(d.xComponent(), d.yComponent());
    }

    protected void move(int dx, int dy) throws EntityPositionOutOfBoundsException {
        Point copy = new Point(pos);
        copy.translate(dx, dy);

        //To prevent negative coordinates entering the array indices at Board.grid[][]
        if(copy.x < 0 || copy.y < 0 || copy.x >= Main.board.getColumns() || copy.y >= Main.board.getRows()) {
            throw new EntityPositionOutOfBoundsException(
                    String.format("An entity wanted to move out of the bounds of the map: %s was out of bounds (0, 0) to (%d, %d)", copy, Main.board.getColumns(), Main.board.getRows())
            );
        }

        pos.move(copy.x, copy.y);
    }

    public int getViewDistance() {
        return VIEW_DISTANCE;
    }
}

