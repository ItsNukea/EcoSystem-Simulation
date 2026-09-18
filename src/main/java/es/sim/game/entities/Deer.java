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
    private Point currentTarget = null;
    private ArrayList<Direction> moves = new ArrayList<>();
    private EntityActivity ACTIVITY = EntityActivity.WANDERING;
    private Surroundings surroundings;

    public Deer() {
        super(Identifier.of("entity:deer"));
        VIEW_DISTANCE = 8;
    }

    @Override
    public void tick() {
        analyzeSurroundings();

        if(moves.isEmpty()) {
            findRandomTarget();
            recalculatePath();
        }

        move(moves.getFirst());
        moves.removeFirst();
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

    /// Finds a random target that lies within the bounds of {@link Main#board}
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

    private void recalculatePath() {
        GridCell[][] cells = surroundings.toGridCellArray();
        NavigationGrid<GridCell> navGrid = new NavigationGrid<>(cells, false);
        //Imagine having options for a gf
        GridFinderOptions gfOptions = new GridFinderOptions();
        gfOptions.allowDiagonal = false;
        gfOptions.isYDown = true;

        AStarGridFinder<GridCell> ASGF = new AStarGridFinder<>(GridCell.class, gfOptions);
        //Now get the Cell origin as a start position and the target Point as an end position:
        int length = cells.length;
        //We know the square MUST have uneven side lengths because there is a center square
        GridCell start = cells[length / 2][length / 2];
        int dx = currentTarget.x - pos.x;
        int dy = currentTarget.y - pos.y;
        GridCell end = cells[length / 2 + dx][length / 2 + dy];

        ArrayList<GridCell> path = (ArrayList<GridCell>) ASGF.findPath(start, end, navGrid);

        moves = new ArrayList<>();
        for(int i = 1; i < path.size(); i++) {
            GridCell next = path.get(i);
            GridCell current = path.get(i - 1);

            int moveX = next.x - current.x;
            int moveY = next.y - current.y;
            moves.add(Direction.fromCoordSet(moveX, moveY));
        }
        int debugVar = 0;
    }

    private void analyzeSurroundings() {
        //Here goes EntityActivity logic. It is decided here what an entity will do a certain tick.
        surroundings = Surroundings.ofEntity(this);
        age++;
        if (breedCooldown > 0) breedCooldown--;

        if(surroundings.entityCountOfType("wolf") != 0) {
            //ACTIVITY = EntityActivity.FLEEING;
            //currentTarget = null;
            if (ACTIVITY == EntityActivity.BREEDING) {
                if (mate == null || mate.isDead()) { clearMate(); }
                return;                       // already committed, keep walking to the mate
            }

            if (canBreed()) {
                for (Entity e : surroundings.entitiesOfType("deer")) {
                    Deer other = (Deer) e;
                    if (other.canBreed() && other.acceptMate(this)) {
                        this.mate = other;
                        this.ACTIVITY = EntityActivity.BREEDING;
                        this.currentTarget = new Point(other.getPos());
                        moves.clear();
                        recalculatePath();
                        return;
                    }
                }
            }
        } else {
            ACTIVITY = EntityActivity.WANDERING;
        }
    }

    private static final int BREEDING_AGE = 60;
    private static final int BREED_COOLDOWN = 300;

    private Deer mate = null;

    public boolean canBreed() {
        return age >= BREEDING_AGE && breedCooldown == 0 && mate == null;
    }

    public boolean acceptMate(Deer suitor) {
        if (!canBreed()) return false;
        this.mate = suitor;
        this.ACTIVITY = EntityActivity.BREEDING;
        return true;
    }
}
