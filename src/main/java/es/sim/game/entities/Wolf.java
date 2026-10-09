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
    private static final int MIN_MEAL_FULLNESS = 150;
    private static final int MAX_MEAL_FULLNESS = 300;

    /// How much stomach fullness a wolf spends on spawning a new wolf
    private static final int REPRODUCTION_COST = 300;

    /// A wolf is ready to breed once its stomach is at least this full ("almost max")
    private static final int BREEDING_FULLNESS_THRESHOLD = 325;
    private static final int MAX_BREEDING_COOLDOWN = 400;

    /// How far beyond VIEW_DISTANCE a wolf can search for a breeding partner. Must stay at or below 2.0 --
    /// the meeting point is the midpoint between the two wolves, so it's never more than half their distance
    /// apart from either one, and a higher multiplier would let that midpoint fall outside VIEW_DISTANCE again
    private static final double BREEDING_VIEW_DISTANCE_MULTIPLIER = 2.0;

    private final Texture sprite = new Texture(getEntityID());
    private Entity prey = null;
    private Wolf breedingPartner = null;
    private final int MAX_STOMACH_FULLNESS = 375;
    private final int HUNGER_THRESHOLD;

    public Wolf() {
        super(Identifier.of("entity:wolf"));
        breedingCooldown = 250;
        stomachFullness = MAX_STOMACH_FULLNESS / 2;
        VIEW_DISTANCE = 8;
        HUNGER_THRESHOLD = 165;
        ACTIVITY = EntityActivity.WANDERING;
        ONSET_AGE = 4000d;
        AGING_RATE = 0.02d;
    }

    @Override
    public void tick() {
        if(Math.random() <= getDeathProbability()) {
            board.unregisterEntity(this);
            return;
        }

        ageUp();
        breedingCooldown = Math.max(0, breedingCooldown - 1);

        analyzeSurroundings();

        if (ACTIVITY == EntityActivity.HUNTING) {
            hunt();
        } else if (ACTIVITY == EntityActivity.BREEDING) {
            seekPartner();
        } else {
            wander();
        }

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
    }

    @Override
    public void renderTarget(Graphics2D graphics, Rectangle cellBounds) {
        //Debug: mark the current target, but not while hunting, because the target is then the deer's own cell
        if (currentTarget != null && DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
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
    }

    @Override
    protected void analyzeSurroundings() {
        surroundings = Surroundings.ofEntity(this);
        breedingPartner = null;

        if (isHungry()) {
            prey = surroundings.getClosestEntityofType(Deer.class);
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

        int normalSpeed = ticksPerMove;
        ticksPerMove = Math.max(1, ticksPerMove / HUNT_SPEED); //hunting is faster than the wolf's normal pace

        if (distanceTo(prey.getPos()) <= 1) {
            ticksPerMove = normalSpeed;
            board.unregisterEntity(prey);
            prey = null;
            moves.clear();
            currentTarget = null;
            Random random = new Random();
            stomachFullness = Math.min(stomachFullness + random.nextInt(MIN_MEAL_FULLNESS, MAX_MEAL_FULLNESS + 1), MAX_STOMACH_FULLNESS);
            return;
        }

        followPath();
        ticksPerMove = normalSpeed;
    }

    private void followPath() {
        if (moves.isEmpty()) return;
        if (!readyToMove()) return;
        move(moves.pollFirst());
    }

    private Wolf findBreedingPartner() {
        //The meeting point is the midpoint between the two wolves, and recalculatePath()'s grid only reaches
        //VIEW_DISTANCE away from this wolf. So the search itself must stay within 2x that, or the midpoint can
        //land outside the grid, and pathfinding silently falls back to a random target instead
        int maxSearchDistance = (int) (VIEW_DISTANCE * BREEDING_VIEW_DISTANCE_MULTIPLIER);

        double shortestDistance = Double.MAX_VALUE;
        Wolf partner = null;

        for(Entity e : board.getEntities()) {
            if(!(e instanceof Wolf wolf) || wolf == this) continue;
            if(!wolf.isReadyToBreed()) continue;

            int dx = Math.abs(wolf.getPos().x - pos.x);
            int dy = Math.abs(wolf.getPos().y - pos.y);
            if (dx > maxSearchDistance || dy > maxSearchDistance) continue;

            double distance = wolf.getPos().distance(this.getPos());
            if(distance < shortestDistance) {
                shortestDistance = distance;
                partner = wolf;
            }
        }
        return partner;
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
        return stomachFullness >= BREEDING_FULLNESS_THRESHOLD && breedingCooldown == 0;
    }

    /// A point exactly between two positions, rounded down. Both wolves in a pair compute the same square
    /// independently, without needing to coordinate
    private static Point meetingPoint(Point a, Point b) {
        return new Point(Math.floorDiv(a.x + b.x, 2), Math.floorDiv(a.y + b.y, 2));
    }

    private void seekPartner() {
        if (Util.manhattanDistance(this.getPos(), breedingPartner.getPos()) == 1) {
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

        Wolf baby = new Wolf();
        double avgGene = (this.getSpeedGene() + partner.getSpeedGene()) / 2.0;
        baby.setSpeedGene(Entity.mutateGene(avgGene));
        board.registerEntity(baby, spawnPos.x, spawnPos.y);

        stomachFullness -= REPRODUCTION_COST;
        partner.stomachFullness -= REPRODUCTION_COST;

        breedingCooldown = MAX_BREEDING_COOLDOWN;
        partner.breedingCooldown = MAX_BREEDING_COOLDOWN;
    }
}