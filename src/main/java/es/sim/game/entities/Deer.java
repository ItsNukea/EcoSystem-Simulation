package es.sim.game.entities;

import es.sim.game.EntityActivity;
import es.sim.game.board.Surroundings;
import es.sim.stats.DeathCause;
import es.sim.texture.Texture;
import es.sim.util.*;

import java.awt.*;
import java.util.Random;

import static es.sim.Main.LOGGER;

public class Deer extends Entity {
    public static final int BREEDING_COOLDOWN_TICKS = 150;
    private static final int MAX_STOMACH_FULLNESS = 130;
    private static final int HUNGER_THRESHOLD = 86;

    private static final int MIN_MEAL_FULLNESS = 15;
    private static final int MAX_MEAL_FULLNESS = 30;

    /// How many times faster a deer moves while fleeing a wolf, same idea as Wolf's HUNT_SPEED
    private static final int FLEE_SPEED = 1;

    /// A deer will not breed while more than this many deer (itself included) are in its view. Without this limit
    /// the deer pile up into one solid block: the deer inside cannot move, never get hungry, and cannot be reached by
    /// wolves, so the block only grows and slows the whole simulation down. The view of a deer is
    /// {@code (2 * VIEW_DISTANCE + 1)^2} = 169 tiles, so 25 means about 15% of the view
    private static final int MAX_DEER_IN_VIEW_TO_BREED = 30;


    private Deer breedingPartner = null;
    private BerryBush targetedBush = null;

    public Deer() {
        super(Identifier.of("entity:deer"));
        stomachFullness = (double) MAX_STOMACH_FULLNESS / 2;
        VIEW_DISTANCE = 6;
        breedingCooldown = BREEDING_COOLDOWN_TICKS;
        ACTIVITY = EntityActivity.WANDERING;
        ONSET_AGE = 1988d;
    }

    /// A point exactly between two positions, rounded down. Since addition is commutative, both deer in a
    /// breeding pair compute this to the same square independently, without needing to coordinate
    private static Point meetingPoint(Point a, Point b) {
        return new Point(Math.floorDiv(a.x + b.x, 2), Math.floorDiv(a.y + b.y, 2));
    }

    @Override
    public void tick() {
        if(Math.random() <= getDeathProbability()) {
            die(DeathCause.OLD_AGE);
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
            if (Util.manhattanDistance(this.getPos(), breedingPartner.getPos()) == 1) {
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
                Random random = board.getRandom();
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
            waitTicks = board.getRandom().nextInt(1, 4);
            return;
        }

        if (pos.equals(currentTarget)) {
            currentTarget = null;
            moves.clear();
        }
        if(stomachFullness <= 0) {
            die(DeathCause.STARVATION);
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

    @Override
    public void renderTarget(Graphics2D graphics, Rectangle cellBounds) {
        if (currentTarget != null && DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(30, 117, 5, 255));
            graphics.fillRect(
                    targetCellBounds.x, targetCellBounds.y,
                    targetCellBounds.width, targetCellBounds.height
            );
        }

        if(ACTIVITY == EntityActivity.BREEDING  && DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
            graphics.setColor(new Color(0, 180, 184));
            graphics.fill(cellBounds);
        }

        if(ACTIVITY == EntityActivity.FLEEING  && DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
            graphics.setColor(new Color(237, 145, 33));
            graphics.fill(cellBounds);
        }
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

        if (surroundings.entityCountOfType(Wolf.class) != 0) {
            Wolf nearestWolf = surroundings.getClosestEntityOfType(Wolf.class);
            if (nearestWolf != null) {
                ACTIVITY = EntityActivity.FLEEING;
                currentTarget = computeFleeTarget(nearestWolf.getPos());
            } else {
                ACTIVITY = EntityActivity.WANDERING;
            }
            breedingPartner = null;
        } else if (isHungry()) {
            BerryBush bush = surroundings.getClosestEntityOfType(BerryBush.class, BerryBush::hasBerries);

            if (bush != null) {
                ACTIVITY = EntityActivity.GATHERING;
            } else {
                ACTIVITY = EntityActivity.WANDERING;
                return;
            }

            currentTarget = bush.pos;
            targetedBush = bush;
            breedingPartner = null;
        } else if (isReadyToBreed() && !isCrowded()) {
            //A crowded deer skips this branch and just wanders, see MAX_DEER_IN_VIEW_TO_BREED
            Deer partner = surroundings.getClosestEntityOfType(Deer.class, Deer::isReadyToBreed);
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

    /// A point {@code VIEW_DISTANCE} away from the deer, in the opposite direction from the wolf, clamped to the board.
    /// Using the full {@code VIEW_DISTANCE} keeps the target right at the edge of the grid recalculatePath() builds
    private Point computeFleeTarget(Point wolfPos) {
        int dx = pos.x - wolfPos.x;
        int dy = pos.y - wolfPos.y;

        if (dx == 0 && dy == 0) {
            //standing on the same square as the wolf; flee in an arbitrary direction instead of dividing by zero
            dx = 1;
        }

        double scale = (double) VIEW_DISTANCE / Math.max(Math.abs(dx), Math.abs(dy));
        Point target = new Point(
                pos.x + (int) Math.round(dx * scale),
                pos.y + (int) Math.round(dy * scale)
        );

        Rectangle bounds = board.getBoundsRect();
        target.x = Math.clamp(target.x, bounds.x, bounds.x + bounds.width - 1);
        target.y = Math.clamp(target.y, bounds.y, bounds.y + bounds.height - 1);

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

    private void breed(Deer partner) {
        if (!partner.isReadyToBreed()) return; //partner already bred with someone else this tick

        Point spawnPos = findEmptyNeighborCell();
        if (spawnPos == null) spawnPos = partner.findEmptyNeighborCell();
        if (spawnPos == null) {
            LOGGER.warn("{} and {} are adjacent and ready, but found no free tile to place a baby", pos, partner.getPos());
            return;
        }

        Deer baby = new Deer();
        double avgGene = (this.getSpeedGene() + partner.getSpeedGene()) / 2.0;
        baby.setSpeedGene(mutateGene(avgGene));
        board.registerEntity(baby, spawnPos.x, spawnPos.y);
        board.deerPopulationCollector.addEntry();

        breedingCooldown = BREEDING_COOLDOWN_TICKS;
        partner.breedingCooldown = BREEDING_COOLDOWN_TICKS;

        breedingPartner = null;
        partner.breedingPartner = null;

        ACTIVITY = EntityActivity.WANDERING;
        partner.ACTIVITY = EntityActivity.WANDERING;
    }

    /// Whether too many deer share this deer's view to breed, see {@link #MAX_DEER_IN_VIEW_TO_BREED}.
    /// Uses the {@link Surroundings} of the current tick, so {@link #analyzeSurroundings()} has to have run first
    /// @return {@code true} if the number of deer in view (this deer included) is above the limit
    private boolean isCrowded() {
        int deerInView = surroundings.entityCountOfType(Deer.class);

        return deerInView > MAX_DEER_IN_VIEW_TO_BREED;
    }

    private boolean isHungry() {
        return stomachFullness <= HUNGER_THRESHOLD;
    }
}