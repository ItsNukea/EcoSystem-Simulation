package es.sim.game.board;

import es.sim.game.entities.Entity;

import java.util.ArrayList;

import static es.sim.Main.LOGGER;

public class Board {
    private Cell[][] grid;
    private ArrayList<Entity> entities = new ArrayList<>();
    private final int rows;
    private final int columns;

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public Cell getCell(int x, int y) {
        if(x < 0 || x >= columns || y < 0 || y >= rows) {
            String message = String.format("Could not provide Cell at coordinate (%d, %d) because the coordinate does not exist", x, y);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }

        return grid[x][y];
    }

    public void setCell(Cell cell, int x, int y) {
        grid[x][y] = cell;
    }

    public void tick() {
        for(Entity entity : entities) {
            entity.tick();
        }
    }
}
