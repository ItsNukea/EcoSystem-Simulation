package es.sim.game.entities;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.texture.*;
import es.sim.util.*;

import java.awt.*;

public class Wolf extends Entity {
    /// How many cells the wolf moves per tick while chasing prey. It needs to be higher than the deer's speed,
    /// otherwise it could never catch a deer that flees
    private static final int HUNT_SPEED = 2;

    private final Texture sprite = new Texture(getEntityID());
    public EntityActivity ACTIVITY = EntityActivity.WANDERING;
    private Entity prey = null;
    private final int MAX_STOMACH_FULLNESS = 75;
    private final int HUNGER_TRESHOLD = 40;
    private int stomachFullness = MAX_STOMACH_FULLNESS;

    public Wolf() {
        super(Identifier.of("entity:wolf"));
        VIEW_DISTANCE = 10;
    }

    @Override
    public void tick() {
        analyzeSurroundings();

        if (ACTIVITY == EntityActivity.HUNTING) {
            hunt();
        } else if(ACTIVITY == EntityActivity.WANDERING) {
            wander();
        }

        stomachFullness--;
        if(stomachFullness == 0) {
            board.unregisterEntity(this);
        }
    }

    @Override
    public void render(Graphics2D graphics, Rectangle cellBounds) {
        graphics.drawImage(
                sprite.asImage(),
                cellBounds.x,
                cellBounds.y,
                cellBounds.width,
                cellBounds.height,
                null
        );

        //Debug: mark the current target, but not while hunting, because the target is then the deer's own cell
        if (currentTarget != null) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(117, 5, 5, 255));
            graphics.fillRect(
                    targetCellBounds.x, targetCellBounds.y,
                    targetCellBounds.width, targetCellBounds.height
            );
        }
    }

    @Override
    protected void analyzeSurroundings() {
        surroundings = Surroundings.ofEntity(this);
        if(isHungry()) {
            prey = findNearestPrey();
        }
        ACTIVITY = (prey != null) ? EntityActivity.HUNTING : EntityActivity.WANDERING;
    }

    /// Finds the closest deer inside the wolf's (square) view area, or {@code null} if there is none.
    /// This scans the board's entity list directly, because {@link Surroundings} can only count entities, not locate them
    private Entity findNearestPrey() {
        Entity nearest = null;
        int nearestDistance = Integer.MAX_VALUE;

        for (Entity other : board.getEntities()) {
            if (!other.getEntityID().getPath().equals("deer")) continue;

            int dx = Math.abs(other.getPos().x - pos.x);
            int dy = Math.abs(other.getPos().y - pos.y);
            if (Math.max(dx, dy) > VIEW_DISTANCE) continue;

            if (dx + dy < nearestDistance) {
                nearest = other;
                nearestDistance = dx + dy;
            }
        }

        return nearest;
    }

    private void wander() {
        if (moves.isEmpty()) {
            findRandomTarget();
            recalculatePath();
        }
        followPath();
    }

    /// The prey keeps moving, so the path is recalculated every tick while hunting
    private void hunt() {
        currentTarget = new Point(prey.getPos());
        recalculatePath();

        for (int i = 0; i < HUNT_SPEED; i++) {
            if (distanceTo(prey.getPos()) <= 1) {
                board.unregisterEntity(prey);
                prey = null;
                moves.clear();
                currentTarget = null;
                stomachFullness = Math.min(stomachFullness + 40, MAX_STOMACH_FULLNESS);
                return;
            }
            followPath();
        }
    }

    private void followPath() {
        if (moves.isEmpty()) return;
        move(moves.pollFirst());
    }

    /// Manhattan distance, which is the number of moves needed since diagonal moves aren't allowed
    private int distanceTo(Point other) {
        return Math.abs(other.x - pos.x) + Math.abs(other.y - pos.y);
    }

    private boolean isHungry() {
        return stomachFullness <= HUNGER_TRESHOLD;
    }
}