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

import static es.sim.Main.LOGGER;

public class Deer extends Entity {
    private static final int BREEDING_COOLDOWN_TICKS = 20;

    private Point currentTarget = null;
    private ArrayDeque<Direction> moves = new ArrayDeque<>();
    private EntityActivity ACTIVITY = EntityActivity.WANDERING;
    private Surroundings surroundings;
    private boolean justReachedTarget = false;

    private int breedingCooldown = BREEDING_COOLDOWN_TICKS;
    private Deer breedingPartner = null;

    public Deer() {
        super(Identifier.of("entity:deer"));
        VIEW_DISTANCE = 6;
    }

    @Override
    public void tick() {
        analyzeSurroundings();

        if (breedingCooldown > 0) {
            breedingCooldown--;
        }

        if (ACTIVITY == EntityActivity.BREEDING && breedingPartner != null) {
            if (isAdjacentTo(breedingPartner.getPos())) {
                breed(breedingPartner);
                return;
            }

            currentTarget = breedingPartner.getPos();
            recalculatePath();
        }

        while(moves.isEmpty()) {
            findRandomTarget();
            recalculatePath();
        }

        move(moves.pollFirst());

        if (pos.equals(currentTarget)) {
            currentTarget = null;
            moves.clear();
            justReachedTarget = true;
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

        if (currentTarget != null) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(30, 117, 5, 255));
            graphics.fillRect(
                    targetCellBounds.x, targetCellBounds.y,
                    targetCellBounds.width, targetCellBounds.height
            );
        }
    }

    public boolean isReadyToBreed() {
        return breedingCooldown <= 0;
    }

    /// Finds a random target that lies within the bounds of {@link Main#board}
    private void findRandomTarget() {
        Point temp;
        while (true) {
            Random random = new Random();
            int dx = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;
            int dy = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;

            //Generate a number from 0 to 2*VIEW DISTANCE + 1

            temp = new Point(getPos().x + dx, getPos().y + dy);
            Rectangle bounds = board.getBoundsRect();

            if (bounds.contains(temp) && !(dx == 0 && dy == 0)) {
                break;
            }
        }

        currentTarget = temp;
    }

    private void recalculatePath() {
        do {
            long startTime = System.nanoTime();
            GridCell[][] cells = surroundings.toGridCellArray();
            NavigationGrid<GridCell> navGrid = new NavigationGrid<>(cells, false);
            GridFinderOptions gfOptions = new GridFinderOptions();
            gfOptions.allowDiagonal = false;
            gfOptions.isYDown = true;

            AStarGridFinder<GridCell> ASGF = new AStarGridFinder<>(GridCell.class, gfOptions);
            int length = cells.length;
            GridCell start = cells[length / 2][length / 2];
            int dx = currentTarget.x - pos.x;
            int dy = currentTarget.y - pos.y;
            GridCell end = cells[length / 2 + dx][length / 2 + dy];

            ArrayList<GridCell> path = (ArrayList<GridCell>) ASGF.findPath(start, end, navGrid);
            if (path == null) {
                LOGGER.error("Failed to pathfind: {}", Surroundings.ofEntity(this));
                //Try again
                continue;
            }

            moves = new ArrayDeque<>();
            for (int i = 1; i < path.size(); i++) {
                GridCell next = path.get(i);
                GridCell current = path.get(i - 1);

                int moveX = next.x - current.x;
                int moveY = next.y - current.y;
                moves.add(Direction.fromCoordSet(moveX, moveY));
            }
            long endTime = System.nanoTime();
            long totalTime = endTime - startTime;
            LOGGER.debug("Calculating path took {} nanoseconds or {} seconds", totalTime, totalTime / 1000000000d);
        } while(false);
    }

    /// This method lets the entity see all tiles that are around him in the form of a {@link Surroundings} instance.<br>
    /// {@link Surroundings} are used to know what the {@link EntityActivity} is that this entity should do this tick.
    @Override
    protected void analyzeSurroundings() {
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

        for (Entity entity : surroundings.getEntitiesOfType("deer")) {
            if (entity == this) continue;

            Deer other = (Deer) entity;
            if (!other.isReadyToBreed()) continue;

            double distance = pos.distance(other.getPos());

            //Stay within VIEW_DISTANCE so recalculatePath()'s grid (sized for VIEW_DISTANCE)
            //never gets asked to path to a cell outside its own array
            if (distance <= VIEW_DISTANCE && distance < closestDistance) {
                closestDistance = distance;
                closest = other;
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