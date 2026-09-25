package es.sim.game.entities;

import ch.qos.logback.core.util.*;
import es.sim.*;
import es.sim.exceptions.*;
import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.awt.*;
import java.util.*;
import java.util.List;

import static es.sim.Main.*;

/// This class represents the super class of all living things on the {@link Board}, every entity should
/// extend this class to be able to be rendered and ticked
public abstract class Entity {
    protected int VIEW_DISTANCE;

    protected Point pos;
    private final Identifier ENTITY_ID;
    protected final Board board;

    protected int age = 0;
    protected int breedingCooldown = 0;
    public Point currentTarget = null;
    protected Surroundings surroundings = null;
    protected ArrayDeque<Direction> moves = new ArrayDeque<>();
    protected int waitTicks = 0;
    
    protected Entity(Identifier entityID) {
        this.ENTITY_ID = entityID;
        this.board = Main.board;
    }

    /// Ticks an entity, making it move one step further in time.
    public abstract void tick();

    /// Renders the entity on the screen.
    /// @param cellBounds The {@link Rectangle} that shows the bounds of the cell this entity is in.
    public abstract void render(Graphics2D graphics, Rectangle cellBounds);

    /// The color of the debug square drawn on this entity's target, or {@code null} for no square
    protected Color getTargetColor() {
        return null;
    }

    /// Draws the debug square on this entity's target. The {@link Board} draws these for every entity before
    /// any sprite, so a sprite is never hidden by another entity's target square
    public void renderTarget(Graphics2D graphics) {
        if (currentTarget == null || getTargetColor() == null) return;

        Rectangle targetCellBounds = board.getCellBounds(currentTarget.x, currentTarget.y);
        graphics.setColor(getTargetColor());
        graphics.fillRect(
                targetCellBounds.x, targetCellBounds.y,
                targetCellBounds.width, targetCellBounds.height
        );
    }

    public Identifier getEntityID() {
        return ENTITY_ID;
    }

    public Point getPos() {
        return pos;
    }

    public void setPos(Point pos) {
        this.pos = pos;
    }
    public void setPos(int x, int y) {
        setPos(new Point(x, y));
    }

    protected boolean move(Direction d) {
        return move(d.xComponent(), d.yComponent());
    }

    protected boolean move(int dx, int dy) {
        Point copy = new Point(pos);
        copy.translate(dx, dy);

        //To prevent negative coordinates entering the array indices at Board.grid[][]
        if(copy.x < 0 || copy.y < 0 || copy.x >= Main.board.getColumns() || copy.y >= Main.board.getRows()) {
            throw new EntityPositionOutOfBoundsException(
                    String.format("An entity wanted to move out of the bounds of the map: %s was out of bounds (0, 0) to (%d, %d)", copy, Main.board.getColumns() - 1, Main.board.getRows() - 1)
            );
        }

        Cell destination = board.getCell(copy.x, copy.y);
        if (destination.holder.isPresent()) {
            //Target square is already occupied, so don't move there
            return false;
        }

        board.getCell(pos.x, pos.y).setContents(null);
        pos.move(copy.x, copy.y);
        destination.setContents(this);
        return true;
    }

    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    protected void analyzeSurroundings() {}
    
    /// Finds a random target that lies within the bounds of {@link Main#board}
    protected void findRandomTarget() {
        Random random = new Random();
        Rectangle bounds = board.getBoundsRect();
        for (int attempt = 0; attempt < 200; attempt++) {
            int dx = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;
            int dy = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;
            Point temp = new Point(getPos().x + dx, getPos().y + dy);

            if (bounds.contains(temp) && !(dx == 0 && dy == 0) && board.getCell(temp.x, temp.y).holder.isEmpty()) {
                currentTarget = temp;
                return;
            }
        }
    }

    protected void recalculatePath() {
        surroundings = Surroundings.ofEntity(this);
        moves = new ArrayDeque<>();

        GridCell[][] cells = surroundings.toGridCellArray();
        int center = cells.length / 2;
        NavigationGrid<GridCell> navGrid = new NavigationGrid<>(cells, false);
        //Imagine having options for a gf
        GridFinderOptions gfOptions = new GridFinderOptions();
        gfOptions.allowDiagonal = false;
        gfOptions.isYDown = true;

        AStarGridFinder<GridCell> ASGF = new AStarGridFinder<>(GridCell.class, gfOptions);

        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = currentTarget.x - pos.x;
            int dy = currentTarget.y - pos.y;
            GridCell start = cells[center][center];
            GridCell end = cells[center + dx][center + dy];

            ArrayList<GridCell> path = (ArrayList<GridCell>) ASGF.findPath(start, end, navGrid);

            if(path == null) {
                LOGGER.warn("{} currently on [{}, {}] could not pathfind, picking another target", ENTITY_ID, pos.x, pos.y);
                findRandomTarget();
                continue;
            }

            path.addFirst(start);

            //findPath() does NOT include the start node: the first node is already the first step
            for (int i = 1; i < path.size(); i++) {
                GridCell next = path.get(i);
                GridCell current = path.get(i - 1);

                int moveX = next.x - current.x;
                int moveY = next.y - current.y;
                moves.add(Direction.fromCoordSet(moveX, moveY));
            }
            return;
        }
    }
}