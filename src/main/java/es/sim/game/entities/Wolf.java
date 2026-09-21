package es.sim.game.entities;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.texture.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Wolf extends Entity {
    /// How many cells the wolf moves per tick while chasing prey. It needs to be higher than the deer's speed,
    /// otherwise it could never catch a deer that flees
    private static final int HUNT_SPEED = 2;

    private final Texture sprite = new Texture(getEntityID());
    private Point currentTarget = null;
    private ArrayList<Direction> moves = new ArrayList<>();
    private EntityActivity ACTIVITY = EntityActivity.WANDERING;
    private Surroundings surroundings;
    private Entity prey = null;

    public Wolf() {
        super(Identifier.of("entity:wolf"));
        VIEW_DISTANCE = 10;
    }

    @Override
    public void tick() {
        analyzeSurroundings();

        if (ACTIVITY == EntityActivity.HUNTING) {
            hunt();
        } else {
            wander();
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
        if (ACTIVITY != EntityActivity.HUNTING && currentTarget != null) {
            Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
            graphics.setColor(new Color(117, 5, 5, 255));
            graphics.fillRect(
                    targetCellBounds.x, targetCellBounds.y,
                    targetCellBounds.width, targetCellBounds.height
            );
        }
    }

    private void analyzeSurroundings() {
        surroundings = Surroundings.ofEntity(this);
        prey = findNearestPrey();
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
                moves.clear();
                currentTarget = null;
                return;
            }
            followPath();
        }
    }

    private void followPath() {
        if (moves.isEmpty()) return;

        move(moves.getFirst());
        moves.removeFirst();
    }

    /// Manhattan distance, which is the number of moves needed since diagonal moves aren't allowed
    private int distanceTo(Point other) {
        return Math.abs(other.x - pos.x) + Math.abs(other.y - pos.y);
    }

    /// Finds a random target that lies within the bounds of the board (same as Deer)
    private void findRandomTarget() {
        Point temp;
        while (true) {
            int dx = (int) Math.floor((Math.random() - 0.5d) * 2 * VIEW_DISTANCE);
            int dy = (int) Math.floor((Math.random() - 0.5d) * 2 * VIEW_DISTANCE);

            temp = new Point(getPos().x + dx, getPos().y + dy);
            Rectangle bounds = board.getBoundsRect();

            //Break out if these conditions are met
            if (bounds.contains(temp) && !(dx == 0 && dy == 0)) {
                break;
            }
        }

        currentTarget = temp;
    }

    /// A* from the center of the surroundings grid to {@link #currentTarget} (same as Deer)
    private void recalculatePath() {
        GridCell[][] cells = surroundings.toGridCellArray();
        NavigationGrid<GridCell> navGrid = new NavigationGrid<>(cells, false);
        GridFinderOptions gfOptions = new GridFinderOptions();
        gfOptions.allowDiagonal = false;
        gfOptions.isYDown = true;

        AStarGridFinder<GridCell> ASGF = new AStarGridFinder<>(GridCell.class, gfOptions);
        //The entity is always the center square of the grid, the target is offset from it
        int length = cells.length;
        GridCell start = cells[length / 2][length / 2];
        int dx = currentTarget.x - pos.x;
        int dy = currentTarget.y - pos.y;
        GridCell end = cells[length / 2 + dx][length / 2 + dy];

        List<GridCell> path = ASGF.findPath(start, end, navGrid);

        moves = new ArrayList<>();
        if (path == null) return;

        for (int i = 1; i < path.size(); i++) {
            GridCell next = path.get(i);
            GridCell current = path.get(i - 1);

            int moveX = next.x - current.x;
            int moveY = next.y - current.y;
            moves.add(Direction.fromCoordSet(moveX, moveY));
        }
    }
}