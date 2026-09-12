package es.sim.game.board;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.gui.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static es.sim.Main.*;

/// This class represents the actual playing field of the game. It contains all {@link Cell} data, together with all
/// registered entities.<br>
/// Its task is ticking every entity with some helper methods
public class Board extends Screen {
    public static final int DEFAULT_ROWS = 100;
    public static final int DEFAULT_COLUMNS = 100;

    private static final float MIN_ZOOM = 0.125f;
    private static final float MAX_ZOOM = 8.0f;
    private static final float ZOOM_FACTOR = 1.1f;

    private static float MAX_PAN_X = 0;
    private static float MAX_PAN_Y = 0;

    private final Cell[][] grid;
    private final ArrayList<Entity> entities = new ArrayList<>();
    private final int rows;
    private final int columns;

    private float zoom = 1.0f;
    private float cellSize;

    ///The top left point of the grid
    private float originX, originY;

    private int panX = 0, panY = 0;
    private Point lastMousePos = null;

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

    /// Increases the zoom level by one step, clamped to {@link #MAX_ZOOM}
    public void zoomIn() {
        zoom = Math.min(MAX_ZOOM, ZOOM_FACTOR * zoom);
    }

    /// Decreases the zoom level by one step, clamped to {@link #MIN_ZOOM}
    public void zoomOut() {
        zoom = Math.max(MIN_ZOOM, zoom / ZOOM_FACTOR);
    }

    /// Directly sets the zoom level, clamped to the allowed range
    public void setZoom(float zoom) {
        this.zoom = Math.clamp(zoom, MIN_ZOOM, MAX_ZOOM);
    }

    public float getZoom() {
        return zoom;
    }

    /// Recomputes {@link #cellSize} and the centering origin so the grid fits the window
    /// as large as possible at the current zoom level, then centers it
    private void recalcGrid() {
        int width = Main.getWindow().getWidth();
        int height = Main.getWindow().getHeight();

        cellSize = Math.min((float) width / columns, (float) height / rows) * zoom;

        float gridWidth = columns * cellSize;
        float gridHeight = rows * cellSize;

        MAX_PAN_X = (gridWidth + width) / 2f;
        MAX_PAN_Y = (gridHeight + height) / 2f;

        originX = (width - gridWidth) / 2f + panX;
        originY = (height - gridHeight) / 2f + panY;
    }

    /// Returns the integer pixel bounds of cell (x, y), with edges that always line up
    /// exactly with neighboring cells regardless of rounding
    private Rectangle getCellBounds(int x, int y) {
        int x0 = Math.round(originX + x * cellSize);
        int x1 = Math.round(originX + (x + 1) * cellSize);
        int y0 = Math.round(originY + y * cellSize);
        int y1 = Math.round(originY + (y + 1) * cellSize);
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    @Override
    public void render(Graphics2D g) {
        recalcGrid();

        LOGGER.debug("Grid origin: ({}, {})", originX, originY);

        //Draw a grid pattern
        boolean doGray = false;
        Color gray = new Color(92, 92, 92);
        Color white = new Color(255, 255, 255);

        for(int x = 0; x < columns; x++) {
            boolean rowStart = doGray;
            for(int y = 0; y < rows; y++) {
                Rectangle bounds = getCellBounds(x, y);
                g.setColor(doGray ? gray : white);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                doGray = !doGray;
            }
            doGray = !rowStart;
        }

        for(Entity e : entities) {
            e.render(g);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMousePos = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(lastMousePos != null) {
            int dx = e.getX() - lastMousePos.x;
            int dy = e.getY() - lastMousePos.y;

            LOGGER.debug("Dragging mouse. dx = {}, dy = {}", dx, dy);

            panX = (int) Math.clamp((float) (panX + dx), -MAX_PAN_X, MAX_PAN_X);
            panY = (int) Math.clamp((float) (panY + dy), -MAX_PAN_Y, MAX_PAN_Y);

            lastMousePos = e.getPoint();
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastMousePos = null;
        LOGGER.debug("Releasing mouse");
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(e.isControlDown()) {
            int rotation = e.getWheelRotation();

            for(int n = 0; n < Math.abs(rotation); n++) {
                if(rotation < 0) {
                    zoomIn();
                } else {
                    zoomOut();
                }
                repaint();
            }
        }
    }
}