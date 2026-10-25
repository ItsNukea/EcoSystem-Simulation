package es.sim.game.board;

import es.sim.*;
import es.sim.game.*;
import es.sim.game.entities.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;

import java.awt.*;
import java.util.*;
import java.util.function.*;

/// This class has utility methods to get information about the surroundings of an entity. Used for entity pathfinding
public class Surroundings {
    /// The grid cells that {@link #toGridCellArray()} hands out, kept per view size and per thread. Building them is
    /// expensive (every {@link GridCell} creates two hash maps), so they are reused instead of rebuilt on every
    /// path search. One pool per thread, so simulations ticking on different threads never share cells
    private static final ThreadLocal<HashMap<Integer, GridCell[][]>> GRID_POOL = ThreadLocal.withInitial(HashMap::new);

    public Entity owner;

    /// The 2D-grid of {@link Cell} objects that are visible to the entity. The value corresponding to
    /// {@code surroundings[0][0]} is the most upper left {@link Cell} of the visible area
    private final Cell[][] surroundings;

    private Surroundings(Entity owner, Cell[][] surroundings) {
        this.owner = owner;
        this.surroundings = surroundings;
    }

    /// Returns the surrounding Cells of an {@link Entity}. This is the main way to get a {@link Surroundings} instance
    public static Surroundings ofEntity(Entity e) {
        Simulation sim = Main.simulation;

        int viewDiameter = e.getViewDistance() * 2 + 1;

        Cell[][] surroundings = new Cell[viewDiameter][viewDiameter];
        //Created once here instead of once per cell: Board#getBoundsRect() makes a new Rectangle on every call
        Rectangle bounds = sim.getBoundsRect();

        for(int x = 0; x < viewDiameter; x++) {
            for(int y = 0; y < viewDiameter; y++) {
                //Check if the board position exists
                int absoluteX = e.getPos().x + x - e.getViewDistance();
                int absoluteY = e.getPos().y + y - e.getViewDistance();

                if (bounds.contains(absoluteX, absoluteY)) {
                    Cell cell = sim.getCell(absoluteX, absoluteY);
                    surroundings[x][y] = cell;
                } else {
                    surroundings[x][y] = null;
                }
            }
        }

        return new Surroundings(e, surroundings);
    }

    /// Counts the number of entities within its {@link Surroundings}
    public int entityCountOfType(Class<? extends Entity> entityClass) {
        int count = 0;

        for(Cell[] arr : surroundings) {
            for(Cell cell : arr) {
                if (cell == null) continue;

                Optional<Entity> holder = cell.holder;
                if(holder.isEmpty()) continue;
                
                if(holder.get().getClass().equals(entityClass)) count++;
            }
        }

        return count;
    }

    @SafeVarargs
    public final <T extends Entity> T getClosestEntityOfType(Class<T> entityClass, Predicate<T>... checks) {
        T closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Cell[] arr : surroundings) {
            middle:
            for (Cell cell : arr) {
                if (cell == null || cell.holder.isEmpty()) continue;

                Entity entity = cell.holder.get();

                if (entity.equals(owner) || !entityClass.isInstance(entity)) {
                    continue;
                }

                T typedEntity = entityClass.cast(entity);

                for (Predicate<T> check : checks) {
                    if (!check.test(typedEntity)) {
                        continue middle;
                    }
                }

                double distance = owner.getPos().distance(typedEntity.getPos());

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = typedEntity;
                }
            }
        }

        return closest;
    }

    /// Returns all entities visible in its Surroundings whose {@link Class} equals the given {@link Class}
    public <T extends Entity> ArrayList<T> getEntitiesOfType(Class<T> entityClass) {
        ArrayList<T> result = new ArrayList<>();

        for(Cell[] arr : surroundings) {
            for(Cell cell : arr) {
                if (cell == null) continue;

                Optional<Entity> holder = cell.holder;
                if(holder.isEmpty() || holder.get().equals(owner)) continue;

                Entity entity = holder.get();
                if(entityClass.isInstance(entity)) {
                    result.add(entityClass.cast(entity));
                }
            }
        }

        return result;
    }

    public Cell[][] toCellArray() {
        return surroundings;
    }

    ///Transforms this Surroundings instance into a 2D-GridCell array, used for navigating a {@link NavigationGrid}.
    ///The cells come from {@link #GRID_POOL} and are reused, so the returned array is only valid until the next call
    ///on the same thread: use it for one search and do not keep it
    public GridCell[][] toGridCellArray() {
        int diameter = owner.getViewDistance() * 2 + 1;
        //Takes the cells for this view size from the pool, and only builds them the first time
        GridCell[][] grid = GRID_POOL.get().computeIfAbsent(diameter, size -> {
            GridCell[][] created = new GridCell[size][size];
            for(int x = 0; x < size; x++) {
                for(int y = 0; y < size; y++) {
                    created[x][y] = new GridCell(x, y);
                }
            }
            return created;
        });

        for(int x = 0; x <= owner.getViewDistance() * 2; x++) {
            for(int y = 0; y <= owner.getViewDistance() * 2; y++) {
                Cell cell = surroundings[x][y];
                GridCell gridCell = grid[x][y];
                gridCell.setWalkable(true); //reset: this cell was used by an earlier search

                boolean thisEntity =
                        x == owner.getViewDistance()
                        && y == owner.getViewDistance();

                if (cell == null || (cell.holder.isPresent() && (!thisEntity))) {
                    gridCell.setWalkable(false);
                }

                if ((owner instanceof Wolf wolf && wolf.ACTIVITY == EntityActivity.HUNTING)
                  || (owner instanceof Deer deer && deer.ACTIVITY == EntityActivity.GATHERING)) {
                    Point target = owner.currentTarget;

                    int absoluteX = owner.getPos().x + x - owner.getViewDistance();
                    int absoluteY = owner.getPos().y + y - owner.getViewDistance();

                    if (absoluteX == target.x && absoluteY == target.y) {
                        gridCell.setWalkable(true);
                    }
                }

                grid[x][y] = gridCell;
            }
        }

        return grid;
    }
}