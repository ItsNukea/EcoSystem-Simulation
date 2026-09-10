package es.sim.game.board;

import es.sim.game.entities.Entity;

import java.util.ArrayList;

public class Board {
    private Cell[][] grid;
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final int rows;
    private final int columns;

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
    }

    public Cell getCell(int x, int y) {
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
