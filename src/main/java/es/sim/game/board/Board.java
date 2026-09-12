package es.sim.game.board;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.gui.*;

import java.awt.*;
import java.util.*;

import static es.sim.Main.*;

/// This class represents the actual playing field of the game. It contains all {@link Cell} data, together with all
/// registered entities.<br>
/// Its task is ticking every entity with some helper methods
public class Board extends Screen {
    public static final int DEFAULT_ROWS = 500;
    public static final int DEFAULT_COLUMNS = 500;

    private final Cell[][] grid;
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final int rows;
    private final int columns;

    public Board(int rows, int columns, Screen parent) {
        super(parent);
        this.rows = rows;
        this.columns = columns;
        grid = new Cell[columns][rows];

        for(int x = 0; x < columns; x++) {
            for(int y = 0; y < rows; y++) {
                grid[x][y] = new Cell(x, y);
            }
        }
    }

    /// Gets a cell at a specified coordinate {@code (x, y)}
    public Cell getCell(int x, int y) {
        if(x < 0 || x >= columns || y < 0 || y >= rows) {
            String message = String.format("Could not provide Cell at coordinate (%d, %d) because the coordinate does not exist", x, y);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }

        return grid[x][y];
    }

    /// Registers an entity so that it can be ticked and rendered on the board
    public void registerEntity(Entity e) {
        entities.add(e);
    }

    /// Unregisters an entity to stop ticking and rendering it on the board. This will mostly be used when an entity dies
    public void unregisterEntity(Entity e) {
        entities.remove(e);
    }

    /// Ticks every registered entity and makes them advance 1 step into the future
    public void tick() {
        for(Entity entity : entities) {
            entity.tick();
        }
    }

    @Override
    public void render(Graphics2D g) {
        //Draw a grid pattern
        boolean doGray = false;
        Color gray = new Color(92, 92, 92);
        Color white = new Color(255, 255, 255);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int minY = 0;
        int maxY = Main.getWindow().getHeight();
        int gridSize = maxY / rows;

        int minX = (screenSize.width - gridSize * rows) / 2;

        for(int x = 0; x < columns; x++) {
            for(int y = 0; y < rows; y++) {
                g.setColor(doGray ? gray : white);
                g.fillRect(minX + x * gridSize, minY + y * gridSize, gridSize, gridSize);
                doGray = !doGray;
            }
            doGray = !doGray;
        }

        for(Entity e : entities) {
            e.render(g);
        }
    }
}
