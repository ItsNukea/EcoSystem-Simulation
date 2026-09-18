package es.sim.game.board;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.util.*;
import org.xguzm.pathfinding.grid.*;

import java.util.*;

/// This class has utility methods to get information about the surroundings of an entity. Used for entity pathfinding
public class Surroundings {
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
        Board board = Main.board;

        int viewDiameter = e.getViewDistance() * 2;

        Cell[][] surroundings = new Cell[viewDiameter + 1][viewDiameter + 1];

        for(int x = 0; x < viewDiameter + 1; x++) {
            for(int y = 0; y < viewDiameter + 1; y++) {
                //Check if the board position exists
                if (board.getBoundsRect().contains(e.getPos().x + x, e.getPos().y + y)) {
                    Cell cell = board.getCell(e.getPos().x + x, e.getPos().y + y);
                    surroundings[x][y] = cell;
                } else {
                    surroundings[x][y] = null;
                }
            }
        }

        return new Surroundings(e, surroundings);
    }

    /// Counts the number of entities within its {@link Surroundings}
    public int entityCountOfType(String entityIdentifierPath) {
        int count = 0;

        for(Cell[] arr : surroundings) {
            for(Cell cell : arr) {
                if (cell == null) continue;

                Optional<Entity> holder = cell.holder;
                if(holder.isEmpty()) continue;
                Identifier containerID = holder.get().getEntityID();
                if(containerID.getPath().equals(entityIdentifierPath)) count++;
            }
        }

        return count;
    }

    public Cell[][] toCellArray() {
        return surroundings;
    }

    ///Transforms this Surroundings instance into a 2D-GridCell array, used for navigating a {@link NavigationGrid}
    public GridCell[][] toGridCellArray() {
        GridCell[][] grid = new GridCell[owner.getViewDistance() * 2 + 1][owner.getViewDistance() * 2 + 1];
        //x  x  x
        //x  x  x
        //x  x  x

        for(int x = 0; x <= owner.getViewDistance() * 2; x++) {
            for(int y = 0; y <= owner.getViewDistance() * 2; y++) {
                Cell cell = surroundings[x][y];
                GridCell gridCell = new GridCell(x, y);
                boolean thisEntity =
                        x == owner.getViewDistance() + 1
                        && y == owner.getViewDistance() + 1;

                if(cell.holder.isPresent() && thisEntity) {
                    gridCell.setWalkable(false);
                }

                grid[x][y] = gridCell;
            }
        }

        return grid;
    }
}
