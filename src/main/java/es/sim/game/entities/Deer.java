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
    private static final int BREEDING_COOLDOWN_TICKS = 50;
    
    private EntityActivity ACTIVITY = EntityActivity.WANDERING;

    private Deer breedingPartner = null;

    public Deer() {
        super(Identifier.of("entity:deer"));
        VIEW_DISTANCE = 6;
        breedingCooldown = BREEDING_COOLDOWN_TICKS;
    }

    /// A point exactly between two positions, rounded down. Since addition is commutative, both deer in a
    /// breeding pair compute this to the same square independently, without needing to coordinate
    private static Point meetingPoint(Point a, Point b) {
        return new Point(Math.floorDiv(a.x + b.x, 2), Math.floorDiv(a.y + b.y, 2));
    }

    @Override
    public void tick() {
        analyzeSurroundings();

        breedingCooldown = Math.max(0, breedingCooldown - 1);

        if (waitTicks > 0) {
            waitTicks--;
            return;
        }

        if (ACTIVITY == EntityActivity.BREEDING && breedingPartner != null) {
            if (isAdjacentTo(breedingPartner.getPos())) {
                breed(breedingPartner);
                return;
            }

            currentTarget = meetingPoint(pos, breedingPartner.getPos());
            recalculatePath();
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

        if (!move(moves.pollFirst())) {
            moves.clear();
            waitTicks = new Random().nextInt(1, 4); //wait 1-3 ticks so two deer facing each other stop mirroring
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
            ACTIVITY = EntityActivity.WANDERING; //Fleeing is not yet implemented
            breedingPartner = null;
        } else if (isReadyToBreed()) {
            Deer partner = findBreedingPartner();
            if (partner != null) {
                breedingPartner = partner;
                ACTIVITY = EntityActivity.BREEDING;
            } else {
                breedingPartner = null;
                ACTIVITY = EntityActivity.WANDERING;
            }
        } else {
            breedingPartner = null;
            ACTIVITY = EntityActivity.WANDERING;
        }
    }

    private Deer findBreedingPartner() {
        Deer closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Deer entity : surroundings.getEntitiesOfType(Deer.class)) {
            if (entity == this) continue;
	        
	        if (!entity.isReadyToBreed()) continue;

            double distance = pos.distance(entity.getPos());

            //Stay within VIEW_DISTANCE so recalculatePath()'s grid (sized for VIEW_DISTANCE)
            //never gets asked to path to a cell outside its own array
            if (distance <= VIEW_DISTANCE && distance < closestDistance) {
                closestDistance = distance;
                closest = entity;
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
        if (spawnPos == null) return; //no free tile for a baby right now, try again later

        Deer baby = new Deer();
        board.registerEntity(baby, spawnPos.x, spawnPos.y);

        breedingCooldown = BREEDING_COOLDOWN_TICKS;
        partner.breedingCooldown = BREEDING_COOLDOWN_TICKS;

        breedingPartner = null;
        partner.breedingPartner = null;

        ACTIVITY = EntityActivity.WANDERING;
        partner.ACTIVITY = EntityActivity.WANDERING;
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
}