package es.sim.game.entities;

import es.sim.*;
import es.sim.exceptions.*;
import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;
import org.xguzm.pathfinding.grid.finders.*;

import java.awt.*;
import java.util.*;

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
    protected Point currentTarget = null;
    protected Surroundings surroundings = null;
    protected ArrayDeque<Direction> moves = new ArrayDeque<>();
    
    
    protected Entity(Identifier entityID) {
        this.ENTITY_ID = entityID;
        this.board = Main.board;
    }

    /// Ticks an entity, making it move one step further in time.
    public abstract void tick();

    /// Renders the entity on the screen.
    /// @param cellBounds The {@link Rectangle} that shows the bounds of the cell this entity is in.
    public abstract void render(Graphics2D graphics, Rectangle cellBounds);

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

    protected void move(Direction d) {
        move(d.xComponent(), d.yComponent());
    }

    protected void move(int dx, int dy) throws EntityPositionOutOfBoundsException {
        Point copy = new Point(pos);
        copy.translate(dx, dy);

        //To prevent negative coordinates entering the array indices at Board.grid[][]
        if(copy.x < 0 || copy.y < 0 || copy.x >= Main.board.getColumns() || copy.y >= Main.board.getRows()) {
            throw new EntityPositionOutOfBoundsException(
                    String.format("An entity wanted to move out of the bounds of the map: %s was out of bounds (0, 0) to (%d, %d)", copy, Main.board.getColumns() - 1, Main.board.getRows() - 1)
            );
        }

        board.getCell(pos.x, pos.y).setContents(null);
        pos.move(copy.x, copy.y);
        board.getCell(copy.x, copy.y).setContents(this);
    }

    public int getViewDistance() {
        return VIEW_DISTANCE;
    }

    protected void analyzeSurroundings() {}
    
    /// Finds a random target that lies within the bounds of {@link Main#board}
    protected void findRandomTarget() {
        Point temp;
        while (true) {
            Random random = new Random();
            int dx = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;
            int dy = random.nextInt(0, 2 * VIEW_DISTANCE + 1) - VIEW_DISTANCE;
            
            //Generate a number from 0 to 2*VIEW DISTANCE + 1
            
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
    protected void recalculatePath() {
        surroundings = Surroundings.ofEntity(this);
        DebugVariables.TIMES_RECALCULATED_THIS_FRAME++;
        boolean repeat;
        do {
            repeat = false;
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
            if (path == null) {
                //It's impossible to pathfind to the target
                LOGGER.error("Entity currently on [{}, {}] failed to pathfind", this.pos.x, this.pos.y);
                findRandomTarget();
                repeat = true;
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
        } while(repeat);
    }
}