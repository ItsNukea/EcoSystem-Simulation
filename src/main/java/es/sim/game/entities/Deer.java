package es.sim.game.entities;

import es.sim.*;
import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.texture.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.awt.*;
import java.util.*;

public class Deer extends Entity {
    private static final int BREEDING_COOLDOWN_TICKS = 200; //5x: same real-world cooldown at the new, 5x higher tps
    private static final int MAX_STOMACH_FULLNESS = 100;
    private static final int HUNGER_THRESHOLD = 90;

    private static final int MIN_MEAL_FULLNESS = 20;
    private static final int MAX_MEAL_FULLNESS = 40;

    private int stomachFullness = MAX_STOMACH_FULLNESS / 2;

    /// How many times faster a deer moves while fleeing a wolf, same idea as Wolf's HUNT_SPEED
    private static final int FLEE_SPEED = 1;


    private Deer breedingPartner = null;
    private BerryBush targetedBush = null;

    public Deer() {
        super(Identifier.of("entity:deer"));
        VIEW_DISTANCE = 6;
        breedingCooldown = BREEDING_COOLDOWN_TICKS;
        ACTIVITY = EntityActivity.WANDERING;
        onsetAge = 400d;
    }

    /// A point exactly between two positions, rounded down. Since addition is commutative, both deer in a
    /// breeding pair compute this to the same square independently, without needing to coordinate
    private static Point meetingPoint(Point a, Point b) {
        return new Point(Math.floorDiv(a.x + b.x, 2), Math.floorDiv(a.y + b.y, 2));
    }

    @Override
    public void tick() {
        if(Math.random() <= getDeathProbability()) {
            board.unregisterEntity(this);
            return;
        }

        ageUp();

        analyzeSurroundings();

        breedingCooldown = Math.max(0, breedingCooldown - 1);

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (ACTIVITY == EntityActivity.FLEEING) {
            flee();
            return;
        }

        if (ACTIVITY == EntityActivity.BREEDING && breedingPartner != null) {
            Main.LOGGER.info("{} breeding toward {} at distance {}", pos, breedingPartner.getPos(), pos.distance(breedingPartner.getPos()));

            if (isAdjacentTo(breedingPartner.getPos())) {
                breed(breedingPartner);
                return;
            }

            currentTarget = meetingPoint(pos, breedingPartner.getPos());
            recalculatePath();
        }

        if(ACTIVITY == EntityActivity.GATHERING) {
            if(!targetedBush.hasBerries()) {
                analyzeSurroundings();
                recalculatePath();
            }
            if(pos.distance(targetedBush.getPos()) == 1) {
                targetedBush.eatBerries();
                Random random = new Random();
                stomachFullness = Math.min(stomachFullness + random.nextInt(MIN_MEAL_FULLNESS, MAX_MEAL_FULLNESS + 1), MAX_STOMACH_FULLNESS);
            }
        }

        //Path ran out or was thrown away: keep heading for the same target if we still have one
        if (moves.isEmpty() && currentTarget != null) {
            recalculatePath();
        }
        if (moves.isEmpty()) {
            findRandomTarget();
            recalculatePath();
        }
        if (moves.isEmpty()) return; //boxed in this tick, try again next tick

        if (!readyToMove()) return; //not this one's turn to step

        if (!move(moves.pollFirst())) {
            moves.clear();
            waitTicks = new Random().nextInt(1, 4);
            return;
        }

        if (pos.equals(currentTarget)) {
            currentTarget = null;
            moves.clear();
        }
    }

    @Override
    public void render(Graphics2D graphics, Rectangle cellBounds) {
        if (currentTarget != null && DebugVariables.SHOW_ENTITY_PATHFINDING_TARGET) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(30, 117, 5, 255));
            graphics.fillRect(
                    targetCellBounds.x, targetCellBounds.y,
                    targetCellBounds.width, targetCellBounds.height
            );
        }

        if(ACTIVITY == EntityActivity.BREEDING) {
            graphics.setColor(new Color(0, 180, 184));
            graphics.fill(cellBounds);
        }

        if(ACTIVITY == EntityActivity.FLEEING) {
            graphics.setColor(new Color(237, 145, 33));
            graphics.fill(cellBounds);
        }

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

    @Override
    protected Color getTargetColor() {
        return new Color(30, 117, 5, 255);
    }

    public boolean isReadyToBreed() {
        return breedingCooldown <= 0;
    }

    /// This method lets the entity see all tiles that are around him in the form of a {@link Surroundings} instance.<br>
    /// {@link Surroundings} are used to know what the {@link EntityActivity} is that this entity should do this tick.
    @Override
    protected void analyzeSurroundings() {
        //Here goes EntityActivity logic. It is decided here what an entity will do a certain tick.
        surroundings = Surroundings.ofEntity(this);

        if (surroundings.entityCountOfType("wolf") != 0) {
            Wolf nearestWolf = findNearestWolf();
            if (nearestWolf != null) {
                ACTIVITY = EntityActivity.FLEEING;
                currentTarget = computeFleeTarget(nearestWolf.getPos());
            } else {
                ACTIVITY = EntityActivity.WANDERING;
            }
            breedingPartner = null;
        } else if (isHungry()) {
            BerryBush bush = surroundings.getClosestEntityofType(BerryBush.class, BerryBush::hasBerries);

            if (bush != null) {
                ACTIVITY = EntityActivity.GATHERING;
            } else {
                ACTIVITY = EntityActivity.WANDERING;
                return;
            }

            currentTarget = bush.pos;
            targetedBush = bush;
            breedingPartner = null;
        } else if (isReadyToBreed()) {
            Deer partner = surroundings.getClosestEntityofType(Deer.class, Deer::isReadyToBreed);
            targetedBush = null;
            if (partner != null) {
                breedingPartner = partner;
                ACTIVITY = EntityActivity.BREEDING;
            } else {
                breedingPartner = null;
                ACTIVITY = EntityActivity.WANDERING;
            }
        } else {
            targetedBush = null;
            breedingPartner = null;
            ACTIVITY = EntityActivity.WANDERING;
        }
    }

    private Wolf findNearestWolf() {
        Wolf nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Wolf wolf : surroundings.getEntitiesOfType(Wolf.class)) {
            double distance = pos.distance(wolf.getPos());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = wolf;
            }
        }

        return nearest;
    }

    /// A point VIEW_DISTANCE away from the deer, in the opposite direction from the wolf, clamped to the board.
    /// Using the full VIEW_DISTANCE keeps the target right at the edge of the grid recalculatePath() builds
    private Point computeFleeTarget(Point wolfPos) {
        int dx = pos.x - wolfPos.x;
        int dy = pos.y - wolfPos.y;

        if (dx == 0 && dy == 0) {
            //standing on the same square as the wolf; flee in an arbitrary direction instead of dividing by zero
            dx = 1;
        }

        double scale = VIEW_DISTANCE / Math.max(Math.abs(dx), Math.abs(dy));
        Point target = new Point(
                pos.x + (int) Math.round(dx * scale),
                pos.y + (int) Math.round(dy * scale)
        );

        Rectangle bounds = board.getBoundsRect();
        target.x = Math.max(bounds.x, Math.min(bounds.x + bounds.width - 1, target.x));
        target.y = Math.max(bounds.y, Math.min(bounds.y + bounds.height - 1, target.y));

        return target;
    }

    /// The wolf keeps moving, so the flee path is recalculated every tick, the same way Wolf.hunt() re-chases prey
    private void flee() {
        recalculatePath();
        if (moves.isEmpty()) return; //boxed in this tick, try again next tick

        int normalSpeed = ticksPerMove;
        ticksPerMove = Math.max(1, ticksPerMove / FLEE_SPEED);

        if (readyToMove() && !move(moves.pollFirst())) {
            moves.clear();
        }

        ticksPerMove = normalSpeed;
    }

    private BerryBush findNearestBerryBushWithBerries() {
        BerryBush closest = null;
        double closestDistance = Double.MAX_VALUE;

        for(BerryBush bush : surroundings.getEntitiesOfType(BerryBush.class)) {
            double distance = pos.distance(bush.getPos());

            if (distance <= VIEW_DISTANCE && distance < closestDistance) {
                closestDistance = distance;
                closest = bush;
            }
        }
        return closest;
    }

    private boolean isAdjacentTo(Point other) {
        int dx = Math.abs(pos.x - other.x);
        int dy = Math.abs(pos.y - other.y);
        return dx <= 1 && dy <= 1 && !(dx == 0 && dy == 0);
    }

    private void breed(Deer partner) {
        if (!partner.isReadyToBreed()) return; //partner already bred with someone else this tick

        Point spawnPos = findEmptyNeighborCell();
        if (spawnPos == null) spawnPos = partner.findEmptyNeighborCell();
        if (spawnPos == null) {
            Main.LOGGER.warn("{} and {} are adjacent and ready, but found no free tile to place a baby", pos, partner.getPos());
            return;
        }

        Deer baby = new Deer();
        board.registerEntity(baby, spawnPos.x, spawnPos.y);

        breedingCooldown = BREEDING_COOLDOWN_TICKS;
        partner.breedingCooldown = BREEDING_COOLDOWN_TICKS;

        breedingPartner = null;
        partner.breedingPartner = null;

        ACTIVITY = EntityActivity.WANDERING;
        partner.ACTIVITY = EntityActivity.WANDERING;
    }

    private boolean isHungry() {
        return stomachFullness <= HUNGER_THRESHOLD;
    }
}