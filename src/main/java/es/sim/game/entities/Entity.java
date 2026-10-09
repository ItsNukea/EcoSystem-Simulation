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

import static es.sim.Main.LOGGER;

/// This class represents the super class of all living things on the {@link Board}, every entity should
/// extend this class to be able to be rendered and ticked
public abstract class Entity {
    protected int VIEW_DISTANCE;

    protected Point pos;
    private final Identifier ENTITY_ID;
    protected final Board board;

    protected int age = 0;

    /// Parameters for the age-based death curve. Each species can set its own values in its constructor
    protected double baselineMortality = 0.0025353d;
    protected double AGING_RATE = 0.02d;
    protected double ONSET_AGE = 2000d;
    protected int stomachFullness;

    protected int breedingCooldown = 0;
    public Point currentTarget = null;
    protected Surroundings surroundings = null;
    protected ArrayDeque<Direction> moves = new ArrayDeque<>();
    protected int waitTicks = 0;
    public EntityActivity ACTIVITY;
    /// Ticks needed to cross one tile. 1 = fastest, moves every tick. TPS was raised 5x without changing any
    /// animal's real-world walking speed, so this default of 5 keeps today's speed identical. Will become
    /// a genetic trait later
    protected int ticksPerMove = 5;
    private int moveCooldown = 0;

    /// The single evolving trait. 1.0 is baseline. A higher value means faster movement but a faster-draining
    /// stomach; a lower value is the opposite trade-off. This is the one variable the research question evolves
    protected double speedGene = 1.0;

    /// ticksPerMove at speedGene == 1.0. Kept separate from ticksPerMove itself so applySpeedGene() can be
    /// called more than once (e.g. once at construction, again once the real gene value is known) without compounding
    protected int baseTicksPerMove = 5;

    /// How much speedGene can drift from a parent's value in one generation, as a fraction of its value
    private static final double MUTATION_STRENGTH = 0.08;

    /// Call once per tick before stepping. Returns whether this entity may move this tick
    protected boolean readyToMove() {
        if (moveCooldown > 0) {
            moveCooldown--;
            return false;
        }
        moveCooldown = ticksPerMove - 1;
        return true;
    }
    
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
    public abstract void renderTarget(Graphics2D graphics, Rectangle cellBounds);

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
                    String.format("An entity wanted to move out of the bounds of the map: %s was out of bounds (0, 0) to (%d, %d)",
                            copy,
                            board.getColumns() - 1,
                            board.getRows() - 1
                    )
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
        stomachFullness -= getMetabolicCost();
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

            //The target can end up outside the surroundings grid, e.g. when a detour around another entity takes us away from it
            if (Math.abs(dx) > center || Math.abs(dy) > center) {
                findRandomTarget();
                continue;
            }

            GridCell start = cells[center][center];
            GridCell end = cells[center + dx][center + dy];

            ArrayList<GridCell> path = (ArrayList<GridCell>) ASGF.findPath(start, end, navGrid);

            if(path == null) {
                if(this instanceof Wolf) {
                    LOGGER.warn("{} currently on [{}, {}] could not pathfind while {}, picking another target", ENTITY_ID, pos.x, pos.y, ACTIVITY);
                }
                findRandomTarget();
                continue;
            }

            //findPath() does NOT include the start node
            path.addFirst(start);

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

    protected Point findEmptyNeighborCell() {
        for (Direction d : Direction.values()) {
            Point candidate = new Point(pos.x + d.xComponent(), pos.y + d.yComponent());

            if (!board.getBoundsRect().contains(candidate)) continue;
            if (board.getCell(candidate.x, candidate.y).holder.isEmpty()) {
                return candidate;
            }
        }
        return null;
    }

    /// Gives this entity a random starting age, somewhere in the first half of its life before the death
    /// probability curve really starts climbing. Call this after creating an entity to spawn it already-aged;
    /// newborns created during the simulation should stay at the default age of 0
    public void randomizeAge() {
        age = new Random().nextInt((int) (ONSET_AGE / 2));
    }

    protected void applySpeedGene() {
        ticksPerMove = Math.max(1, (int) Math.round(baseTicksPerMove / speedGene));
    }

    public double getSpeedGene() {
        return speedGene;
    }

    /// Sets this entity's speed gene and immediately recalculates the movement speed that depends on it.
    /// Call this once, right after construction, whether seeding a starting individual or a new baby
    public void setSpeedGene(double value) {
        speedGene = Math.max(0.2, value); //a floor so the gene can never reach zero or go negative
        applySpeedGene();
    }

    /// A mutated copy of a (typically averaged) parent gene value, for passing a trait on to a baby
    public static double mutateGene(double parentGeneValue) {
        double mutationFactor = 1 + (Math.random() * 2 - 1) * MUTATION_STRENGTH;
        return parentGeneValue * mutationFactor;
    }

    protected int getMetabolicCost() {
        return Math.max(1, (int) Math.round(speedGene));
    }

    /// Call once per tick to make this entity grow older
    protected void ageUp() {
        age++;
    }

    /// The probability, per tick, that this entity dies of old age. Rises sharply once age passes onsetAge
    protected double getDeathProbability() {
        return 1 - Math.exp(
                -baselineMortality * Math.exp(AGING_RATE * (age - ONSET_AGE))
        );
    }
}