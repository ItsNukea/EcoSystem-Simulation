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

    /// A wolf is ready to breed once its stomach is at least this full ("almost max")
    private static final int BREEDING_FULLNESS_THRESHOLD = 65;

    private final Texture sprite = new Texture(getEntityID());
    private Entity prey = null;
    private Wolf breedingPartner = null;
    private final int MAX_STOMACH_FULLNESS = 75;
    private final int HUNGER_THRESHOLD;
    private int stomachFullness = MAX_STOMACH_FULLNESS/2;

    public Wolf() {
        super(Identifier.of("entity:wolf"));
        VIEW_DISTANCE = 9;
        HUNGER_THRESHOLD = 30;
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
        } else if (ACTIVITY == EntityActivity.BREEDING) {
            seekPartner();
        } else {
            wander();
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

        if(ACTIVITY == EntityActivity.BREEDING) {
            graphics.setColor(new Color(0, 180, 184));
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
        breedingPartner = null;

        if (isHungry()) {
            prey = findNearestPrey();
        } else if (isReadyToBreed()) {
            breedingPartner = findBreedingPartner();
        }

        if (prey != null) {
            ACTIVITY = EntityActivity.HUNTING;
        } else if (breedingPartner != null) {
            ACTIVITY = EntityActivity.BREEDING;
        } else {
            ACTIVITY = EntityActivity.WANDERING;
        }
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
    private boolean isReadyToBreed() {
        return stomachFullness >= BREEDING_FULLNESS_THRESHOLD;
    }

    private Wolf findBreedingPartner() {
        Wolf closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Entity other : board.getEntities()) {
            if (!(other instanceof Wolf candidate) || candidate == this) continue;
            if (!candidate.isReadyToBreed()) continue;

            double distance = pos.distance(candidate.getPos());

            //Stay within VIEW_DISTANCE so recalculatePath()'s grid never gets asked to path outside its own array
            if (distance <= VIEW_DISTANCE && distance < closestDistance) {
                closestDistance = distance;
                closest = candidate;
            }
        }

        return closest;
    }

    /// A point exactly between two positions, rounded down. Both wolves in a pair compute the same square
    /// independently, without needing to coordinate
    private static Point meetingPoint(Point a, Point b) {
        return new Point(Math.floorDiv(a.x + b.x, 2), Math.floorDiv(a.y + b.y, 2));
    }

    private boolean isAdjacentTo(Point other) {
        int dx = Math.abs(pos.x - other.x);
        int dy = Math.abs(pos.y - other.y);
        return dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0);
    }

    private void seekPartner() {
        if (isAdjacentTo(breedingPartner.getPos())) {
            breed(breedingPartner);
            return;
        }

        currentTarget = meetingPoint(pos, breedingPartner.getPos());
        recalculatePath();
        followPath();
    }

    private void breed(Wolf partner) {
        if (!partner.isReadyToBreed()) return; //partner already bred this tick

        Point spawnPos = findEmptyNeighborCell();
        if (spawnPos == null) return; //no free tile for a baby right now, try again next tick

        board.registerEntity(new Wolf(), spawnPos.x, spawnPos.y);

        stomachFullness -= REPRODUCTION_COST;
        partner.stomachFullness -= REPRODUCTION_COST;
    }

    private double getDeathProbability() {
        double baselineMortality = 0.0025353d;
        double agingRate = 0.3d;
        double onsetAge = 265d;
        return 1 - Math.exp(
                -baselineMortality * Math.exp(agingRate * (age - onsetAge))
        );
    }
}