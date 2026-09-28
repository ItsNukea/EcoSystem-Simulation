package es.sim.game.entities;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.texture.*;
import es.sim.util.*;

import java.awt.*;
import java.util.Random;

public class Wolf extends Entity {
    /// How many cells the wolf moves per tick while chasing prey. It needs to be higher than the deer's speed,
    /// otherwise it could never catch a deer that flees
    private static final int HUNT_SPEED = 2;

    /// A deer restores a random amount of stomach fullness between these two values (both included)
    private static final int MIN_MEAL_FULLNESS = 35;
    private static final int MAX_MEAL_FULLNESS = 60;

    /// How much stomach fullness a wolf spends on spawning a new wolf
    private static final int REPRODUCTION_COST = 40;

    private final Texture sprite = new Texture(getEntityID());
    private Entity prey = null;
    private final int MAX_STOMACH_FULLNESS = 75;
    private final int HUNGER_THRESHOLD;
    private int stomachFullness = MAX_STOMACH_FULLNESS/2;

    public Wolf() {
        super(Identifier.of("entity:wolf"));
        VIEW_DISTANCE = 10;
        HUNGER_THRESHOLD = 25;
        ACTIVITY = EntityActivity.WANDERING;
    }

    @Override
    public void tick() {
        if(Math.random() <= getDeathProbability()) {
            board.unregisterEntity(this);
            return;
        }

        analyzeSurroundings();

        if (ACTIVITY == EntityActivity.HUNTING) {
            hunt();
        } else if(ACTIVITY == EntityActivity.WANDERING) {
            wander();
        }

        if (stomachFullness >= MAX_STOMACH_FULLNESS) {
            reproduce();
        }

        stomachFullness--;
        if(stomachFullness == 0) {
            board.unregisterEntity(this);
        }
    }

    @Override
    public void render(Graphics2D graphics, Rectangle cellBounds) {
        //Debug: mark the current target, but not while hunting, because the target is then the deer's own cell
        if (currentTarget != null && DebugVariables.SHOW_ENTITY_PATHFINDING_TARGET) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(117, 111, 5));
            graphics.fill(targetCellBounds);
        }

        if(ACTIVITY == EntityActivity.HUNTING) {
            graphics.setColor(new Color(138, 6, 6));
            graphics.fill(cellBounds);
        }

        graphics.drawImage(
                sprite.asImage(),
                cellBounds.x,
                cellBounds.y,
                cellBounds.width,
                cellBounds.height,
                null
        );
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
                Random random = new Random();
                stomachFullness = Math.min(stomachFullness + random.nextInt(MIN_MEAL_FULLNESS, MAX_MEAL_FULLNESS + 1), MAX_STOMACH_FULLNESS);                return;
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
        return stomachFullness <= HUNGER_THRESHOLD;
    }

    /// A wolf with a completely full stomach spawns a new wolf on a free neighboring cell and pays for it in
    /// stomach fullness. The baby starts with the fullness the parent is left with, otherwise it would be full
    /// itself and immediately spawn another wolf
    private void reproduce() {
        Point spawnPos = findEmptyNeighborCell();
        if (spawnPos == null) return; //no free tile right now, try again next tick

        stomachFullness -= REPRODUCTION_COST;

        Wolf baby = new Wolf();
        baby.stomachFullness = stomachFullness;
        board.registerEntity(baby, spawnPos.x, spawnPos.y);
    }

    private Point findEmptyNeighborCell() {
        for (Direction d : Direction.values()) {
            Point candidate = new Point(pos.x + d.xComponent(), pos.y + d.yComponent());

            if (!board.getBoundsRect().contains(candidate)) continue;
            if (board.getCell(candidate.x, candidate.y).holder.isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    private double getDeathProbability() {
        double baselineMortality = 0.0025353d;
        double agingRate = 0.3d;
        double onsetAge = 165d;
        return 1 - Math.exp(
                -baselineMortality * Math.exp(agingRate * (age - onsetAge))
        );
    }
}