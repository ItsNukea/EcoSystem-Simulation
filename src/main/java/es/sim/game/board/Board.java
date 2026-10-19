package es.sim.game.board;

import es.sim.game.entities.*;
import es.sim.gui.Screen;
import es.sim.io.StatisticsSaver;
import es.sim.stats.*;
import es.sim.stats.collectors.*;
import es.sim.stats.graphs.*;
import es.sim.util.*;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.*;

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
    private final CopyOnWriteArrayList<Entity> entities = new CopyOnWriteArrayList<>();
    private final HashMap<Identifier, Integer> entityCounts = new HashMap<>();
    private final int rows;
    private final int columns;

    private final StatisticManager statisticManager = new StatisticManager();
    public final PopulationStatCollector<Wolf> wolfPopulationCollector = new PopulationStatCollector<>(Wolf.class);
    public final PopulationStatCollector<Deer> deerPopulationCollector = new PopulationStatCollector<>(Deer.class);
    public final DeathCauseStatCollector<Wolf> wolfDeathCauseCollector = new DeathCauseStatCollector<>(Wolf.class);
    public final DeathCauseStatCollector<Deer> deerDeathCauseCollector = new DeathCauseStatCollector<>(Deer.class);
    public final PopulationGraph deerPopulationGraph = new PopulationGraph("Deer", deerPopulationCollector);
    public final SpeedGeneGraph speedGeneGraph = new SpeedGeneGraph();
    public final JButton pauseResumeButton;
    public final JButton stepForwardButton;
    public final JButton sprintTickButton;
    
    
    private static final int SIMULATION_TICK_LIMIT = 30000;
    private static final double EQUILIBRIUM_THRESHOLD = 0.01; // Gene change smaller than this
    private double lastDeerAvgSpeed = Double.NaN;
    private double lastWolfAvgSpeed = Double.NaN;
    private boolean isInEquilibrium = false;

    private float zoom = 1.0f;
    private float cellSize;
    private int currentTick = 0;

    ///The top left point of the grid
    private float originX, originY;

    private int panX = 0, panY = 0;
    private Point lastMouseHeldPos = null;

    private final long SEED;
    private final Random RANDOM = new Random();

    public Board(Screen parent) {
        this(DEFAULT_ROWS, DEFAULT_COLUMNS, parent);
    }

    public Board(int rows, int columns, Screen parent) {
        super(parent);
        this.rows = rows;
        this.columns = columns;
        grid = new Cell[columns][rows];

        for(int x = 0; x < columns; x++) {
            for(int y = 0; y < rows; y++) {
                grid[x][y] = new Cell();
            }
        }

        this.setLayout(null);

        stepForwardButton = new JButton("Step Forward");
        stepForwardButton.setBounds(134, 10, 120, 30);
        stepForwardButton.addActionListener(_ -> this.tick());
        stepForwardButton.setEnabled(false);

        sprintTickButton = new JButton("Sprint");
        sprintTickButton.setBounds(258, 10, 120, 30);
        sprintTickButton.addActionListener(_ -> {
            tickLoop.sprint(!tickLoop.isSprinting());
            sprintTickButton.setText(tickLoop.isSprinting() ? "Stop Sprinting" : "Sprint");
        });

        pauseResumeButton = new JButton("Pause");
        pauseResumeButton.setBounds(10, 10, 120, 30);
        pauseResumeButton.addActionListener(_ -> togglePause());

        ChartPanel chartPanel = deerPopulationGraph.getChartPanel();
        chartPanel.setBounds(10, 90, 500, 300);
        chartPanel.setVisible(false);

        JButton toggleGraphButton = new JButton("Toggle Population Graph");
        toggleGraphButton.setBounds(10, 50, 244, 30);
        toggleGraphButton.addActionListener(_ -> {
            chartPanel.setVisible(!chartPanel.isVisible());
            revalidate();
            repaint();
        });

        stepForwardButton.setFocusable(false);
        pauseResumeButton.setFocusable(false);
        sprintTickButton.setFocusable(false);
        toggleGraphButton.setFocusable(false);

        add(pauseResumeButton);
        add(stepForwardButton);
        add(sprintTickButton);
        add(toggleGraphButton);
        add(chartPanel);

        ChartPanel speedGenePanel = speedGeneGraph.getChartPanel();
        //Directly below the population graph: that one starts at y = 90 and is 300 tall, plus a 10 pixel gap
        speedGenePanel.setBounds(10, 400, 500, 300);
        speedGenePanel.setVisible(false);
        add(speedGenePanel);

        JButton toggleSpeedGeneButton = new JButton("Toggle SpeedGene Graph");

        toggleSpeedGeneButton.setBounds(258, 50, 244, 30);
        toggleSpeedGeneButton.addActionListener(_ -> {
            speedGenePanel.setVisible(!speedGenePanel.isVisible());
            revalidate();
            repaint();
        });

        toggleSpeedGeneButton.setFocusable(false);
        add(toggleSpeedGeneButton);

        revalidate();

        statisticManager.addAllCollectors(
                wolfPopulationCollector,
                deerPopulationCollector,
                wolfDeathCauseCollector,
                deerDeathCauseCollector
        );

        deerPopulationGraph.addCollector("Wolf", wolfPopulationCollector);

        SEED = RANDOM.nextLong();
        RANDOM.setSeed(SEED);
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
    public void registerEntity(Entity e, int x, int y) {
        e.setPos(x, y);
        entities.add(e);
        grid[x][y].setContents(e);

        if(!entityCounts.containsKey(e.getEntityID())) {
            entityCounts.put(e.getEntityID(), 0);
        }
        entityCounts.put(e.getEntityID(), entityCounts.get(e.getEntityID()) + 1);
    }

    /// Unregisters an entity to stop ticking and rendering it on the board. This will mostly be used when an entity dies
    public void unregisterEntity(Entity e) {
        entities.remove(e);
        entityCounts.put(e.getEntityID(), entityCounts.get(e.getEntityID()) - 1);

        Cell cell = getCell(e.getPos().x, e.getPos().y);
        if (cell.holder.isPresent() && cell.holder.get() == e) {
            cell.setContents(null);
        }
    }

    public CopyOnWriteArrayList<Entity> getEntities() {
        return entities;
    }
    
    public int getCurrentTick() {
        return currentTick;
    }

    /// Ticks every registered entity and makes them advance 1 step into the future
    public void tick() {
        //Check end conditions before ticking
        if(shouldEndSimulation()) {
            saveSimulationStatistics();
            tickLoop.stop();
            return;
        }
        
        AtomicReference<Double> deerSum = new AtomicReference<>(0d);
        AtomicInteger deerCount = new AtomicInteger(0);
        
        AtomicReference<Double> wolfSum = new AtomicReference<>(0d);
        AtomicInteger wolfCount = new AtomicInteger(0);
        
        for(Entity entity : entities) {
            //Removed earlier this tick (e.g. eaten by a wolf), so it must not act anymore
            if (!entities.contains(entity)) continue;
            entity.tick();
            
            if(entity instanceof Wolf wolf) {
                wolfSum.set(wolfSum.get() + wolf.getSpeedGene());
                wolfCount.incrementAndGet();
            } else if(entity instanceof Deer deer) {
                deerSum.set(deerSum.get() + deer.getSpeedGene());
                deerCount.incrementAndGet();
            }
        }
        
        // Update equilibrium tracking (only if we've reached 30K ticks)
        if(currentTick >= SIMULATION_TICK_LIMIT && deerCount.get() > 0 && wolfCount.get() > 0) {
            double deerAvg = deerSum.get() / deerCount.get();
            double wolfAvg = wolfSum.get() / wolfCount.get();
            
            checkEquilibrium(deerAvg, wolfAvg);
        }

        for(StatCollector collector : statisticManager.collectors) {
            if(collector instanceof TimeSeriesCollector tsCollector) tsCollector.prepareForNextTick(currentTick);
        }

        currentTick++;
        SwingUtilities.invokeLater(() -> {
            deerPopulationGraph.updateGraph();
            speedGeneGraph.updateGraph(deerSum.get(), deerCount.get(), wolfSum.get(), wolfCount.get());
        });
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

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }
    
    public Random getRandom() {
        return RANDOM;
    }

    /// Returns the bounds of the grid as a rectangle with a width and height, with coordinate (0, 0)
    public Rectangle getBoundsRect() {
        return new Rectangle(0, 0, columns, rows);
    }

    public StatisticManager getStatistics() {
        return statisticManager;
    }

    public DeathCauseStatCollector<?> getDeathCauseCollector(Class<? extends Entity> type) {
        if (type == Wolf.class) return wolfDeathCauseCollector;
        if (type == Deer.class) return deerDeathCauseCollector;
        return null;
    }

    /// Recomputes {@link #cellSize} and the centering origin so the grid fits the window
    /// as large as possible at the current zoom level, then centers it
    private void recalcGrid() {
        //The size of this panel, not of the window: the window also includes the title bar and borders
        int width = getWidth();
        int height = getHeight();

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
    public Rectangle getCellBounds(int x, int y) {
        int x0 = Math.round(originX + x * cellSize);
        int x1 = Math.round(originX + (x + 1) * cellSize);
        int y0 = Math.round(originY + y * cellSize);
        int y1 = Math.round(originY + (y + 1) * cellSize);
        return new Rectangle(x0, y0, x1 - x0, y1 - y0);
    }

    public long getRandomSeed() {
        return SEED;
    }

    @Override
    public void render(Graphics2D g) {
        recalcGrid();

        //Draw white background
        Color white = new Color(255, 255, 255);
        g.setColor(white);
        for(int x = 0; x < columns; x++) {
            for(int y = 0; y < rows; y++) {
                Rectangle bounds = getCellBounds(x, y);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        //Grid line color
        Color gray = new Color(92, 92, 92);
        g.setColor(gray);

        //Draw vertical lines
        for(int x = 0; x <= columns; x++) {
            Rectangle topBounds = getCellBounds(Math.min(x, columns - 1), 0);
            Rectangle bottomBounds = getCellBounds(Math.min(x, columns - 1), rows - 1);
            int lineX = (x == columns) ? topBounds.x + topBounds.width : topBounds.x;
            g.drawLine(lineX, topBounds.y, lineX, bottomBounds.y + bottomBounds.height);
        }

        //Draw horizontal lines
        for(int y = 0; y <= rows; y++) {
            Rectangle leftBounds = getCellBounds(0, Math.min(y, rows - 1));
            Rectangle rightBounds = getCellBounds(columns - 1, Math.min(y, rows - 1));
            int lineY = (y == rows) ? leftBounds.y + leftBounds.height : leftBounds.y;
            g.drawLine(leftBounds.x, lineY, rightBounds.x + rightBounds.width, lineY);
        }

        //Do some entity culling because why not
        //The visible area is this panel itself, in its own coordinates, not the window's position on the screen
        Rectangle screenRect = new Rectangle(0, 0, getWidth(), getHeight());
        ArrayList<Entity> visible = new ArrayList<>();
        for(Entity e : entities) {
            if(getCellBounds(e.getPos().x, e.getPos().y).intersects(screenRect)) {
                visible.add(e);
            }
        }

        //Target squares first, then sprites, so no sprite is ever hidden behind another entity's target square
        if(DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
            for (Entity e : visible) {
                e.renderTarget(g, getCellBounds(e.getPos().x, e.getPos().y));
            }
        }
        for(Entity e : visible) {
            e.render(g, getCellBounds(e.getPos().x, e.getPos().y));
        }

        //now the overlays:
        //The right edge of this panel, so the text stays inside the visible area when the window has a title bar
        int right = getWidth();
        g.setColor(Color.BLACK);
        g.drawString("Tick #" + currentTick, right - 100, 15);
        g.drawString("Deer Count: " + entityCounts.get(Identifier.of("entity:deer")), right - 100, 35);
        g.drawString("Wolf Count: " + entityCounts.get(Identifier.of("entity:wolf")), right - 100, 55);
    }
    
    public void togglePause() {
        if(tickLoop.isPaused()) {
            resume();
        } else {
            pause();
        }
    }
    
    public void resume() {
        tickLoop.resume();
        pauseResumeButton.setText("Pause");
        stepForwardButton.setEnabled(false);
        sprintTickButton.setEnabled(true);
    }
    
    public void pause() {
        tickLoop.pause();
        pauseResumeButton.setText("Resume");
        stepForwardButton.setEnabled(true);
        sprintTickButton.setEnabled(false);
    }

    @Override
    public void onClose() {
        pause();
        super.onClose();
    }
    
    private boolean shouldEndSimulation() {
        // Check if we've reached 30K ticks AND are in equilibrium
        if(entityCounts.get(Identifier.of("entity:wolf")) == 0) {
            LOGGER.info("Simulation ended: No more wolves");
            return true;
        }
        
        return false;
    }
    
    private void checkEquilibrium(double deerAvg, double wolfAvg) {
        if(Double.isNaN(lastDeerAvgSpeed) || Double.isNaN(lastWolfAvgSpeed)) {
            lastDeerAvgSpeed = deerAvg;
            lastWolfAvgSpeed = wolfAvg;
            isInEquilibrium = false;
            return;
        }
        
        double deerChange = Math.abs(deerAvg - lastDeerAvgSpeed);
        double wolfChange = Math.abs(wolfAvg - lastWolfAvgSpeed);
        
        // Both species must have minimal speed gene change
        isInEquilibrium = (deerChange < EQUILIBRIUM_THRESHOLD && wolfChange < EQUILIBRIUM_THRESHOLD);
        
        lastDeerAvgSpeed = deerAvg;
        lastWolfAvgSpeed = wolfAvg;
    }
    
    private void saveSimulationStatistics() {
        StatisticsSaver.saveSimulationStatistics(this);
    }
    
    @Override
    public void mousePressed(MouseEvent e) {
        lastMouseHeldPos = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if(lastMouseHeldPos != null) {
            int dx = e.getX() - lastMouseHeldPos.x;
            int dy = e.getY() - lastMouseHeldPos.y;

            panX = (int) Math.clamp((float) (panX + dx), -MAX_PAN_X, MAX_PAN_X);
            panY = (int) Math.clamp((float) (panY + dy), -MAX_PAN_Y, MAX_PAN_Y);

            lastMouseHeldPos = e.getPoint();
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        lastMouseHeldPos = null;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int rotation = e.getWheelRotation();

        for (int n = 0; n < Math.abs(rotation); n++) {
            if (rotation < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
            repaint();
        }
    }
}