package es.sim.game.board;

import es.sim.*;
import es.sim.game.Simulation;
import es.sim.game.entities.*;
import es.sim.gui.Screen;
import es.sim.stats.graphs.*;
import es.sim.util.*;
import org.jfree.chart.ChartPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/// The visual representation of a {@link Simulation}: draws the grid and its entities, handles zoom and panning,
/// and hosts the control buttons and graphs. It holds no simulation state of its own
public class Board extends Screen {
    private static final float MIN_ZOOM = 0.125f;
    private static final float MAX_ZOOM = 8.0f;
    private static final float ZOOM_FACTOR = 1.1f;

    private static float MAX_PAN_X = 0;
    private static float MAX_PAN_Y = 0;

    private final Simulation simulation;

    public final PopulationGraph populationGraph;
    public final SpeedGeneGraph speedGeneGraph;
    public final JButton pauseResumeButton;
    public final JButton stepForwardButton;
    public final JButton sprintTickButton;

    private float zoom = 1.0f;
    private float cellSize;

    ///The top left point of the grid
    private float originX, originY;

    private int panX = 0, panY = 0;
    private Point lastMouseHeldPos = null;

    public Board(Screen parent, Simulation simulation) {
        super(parent);
        this.simulation = simulation;

        this.setLayout(null);

        stepForwardButton = new JButton("Step Forward");
        stepForwardButton.setBounds(134, 10, 120, 30);
        stepForwardButton.addActionListener(_ -> simulation.tick());
        stepForwardButton.setEnabled(false);

        sprintTickButton = new JButton("Sprint");
        sprintTickButton.setBounds(258, 10, 120, 30);
        sprintTickButton.addActionListener(_ -> {
            simulation.sprint(!simulation.isSprinting());
            sprintTickButton.setText(simulation.isSprinting() ? "Stop Sprinting" : "Sprint");
        });

        pauseResumeButton = new JButton("Pause");
        pauseResumeButton.setBounds(10, 10, 120, 30);
        pauseResumeButton.addActionListener(_ -> togglePause());

        populationGraph = new PopulationGraph("Deer", simulation.deerPopulationCollector);
        populationGraph.addCollector("Wolf", simulation.wolfPopulationCollector);
        speedGeneGraph = new SpeedGeneGraph(simulation.deerSpeedGeneCollector, simulation.wolfSpeedGeneCollector);

        ChartPanel chartPanel = populationGraph.getChartPanel();
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
        //Directly below the population graph: that one starts at y = 90 and is 300 tall, plus a 10-pixel gap
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

        simulation.addTickListener(this::updateGraphs);
    }

    private void updateGraphs() {
        //do something
        if(simulation.getCurrentTick() % 50 == 0) {
            populationGraph.updateGraph();
            speedGeneGraph.updateGraph();
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
        int columns = simulation.getColumns();
        int rows = simulation.getRows();

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

    @Override
    public void render(Graphics2D g) {
        recalcGrid();

        int columns = simulation.getColumns();
        int rows = simulation.getRows();

        //Draw white background
        Color white = new Color(255, 255, 255);
        g.setColor(white);
        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                Rectangle bounds = getCellBounds(x, y);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }

        //Grid line color
        Color gray = new Color(92, 92, 92);
        g.setColor(gray);

        //Draw vertical lines
        for (int x = 0; x <= columns; x++) {
            Rectangle topBounds = getCellBounds(Math.min(x, columns - 1), 0);
            Rectangle bottomBounds = getCellBounds(Math.min(x, columns - 1), rows - 1);
            int lineX = (x == columns) ? topBounds.x + topBounds.width : topBounds.x;
            g.drawLine(lineX, topBounds.y, lineX, bottomBounds.y + bottomBounds.height);
        }

        //Draw horizontal lines
        for (int y = 0; y <= rows; y++) {
            Rectangle leftBounds = getCellBounds(0, Math.min(y, rows - 1));
            Rectangle rightBounds = getCellBounds(columns - 1, Math.min(y, rows - 1));
            int lineY = (y == rows) ? leftBounds.y + leftBounds.height : leftBounds.y;
            g.drawLine(leftBounds.x, lineY, rightBounds.x + rightBounds.width, lineY);
        }

        //Do some entity culling because why not
        //The visible area is this panel itself, in its own coordinates, not the window's position on the screen
        Rectangle screenRect = new Rectangle(0, 0, getWidth(), getHeight());
        ArrayList<Entity> visible = new ArrayList<>();
        for (Entity e : simulation.getEntities()) {
            if (getCellBounds(e.getPos().x, e.getPos().y).intersects(screenRect)) {
                visible.add(e);
            }
        }

        //Target squares first, then sprites, so no sprite is ever hidden behind another entity's target square
        if (DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION) {
            for (Entity e : visible) {
                e.renderTarget(g, getCellBounds(e.getPos().x, e.getPos().y));
            }
        }
        for (Entity e : visible) {
            e.render(g, getCellBounds(e.getPos().x, e.getPos().y));
        }

        //now the overlays:
        //The right edge of this panel, so the text stays inside the visible area when the window has a title bar
        int right = getWidth();
        g.setColor(Color.BLACK);
        g.drawString("Tick #" + simulation.getCurrentTick(), right - 100, 15);
        g.drawString("Deer Count: " + simulation.getEntityCount(Identifier.of("entity:deer")), right - 100, 35);
        g.drawString("Wolf Count: " + simulation.getEntityCount(Identifier.of("entity:wolf")), right - 100, 55);
    }

    public void togglePause() {
        if (simulation.isPaused()) {
            resume();
        } else {
            pause();
        }
    }

    public void resume() {
        simulation.resume();
        pauseResumeButton.setText("Pause");
        stepForwardButton.setEnabled(false);
        sprintTickButton.setEnabled(true);
    }

    public void pause() {
        simulation.pause();
        pauseResumeButton.setText("Resume");
        stepForwardButton.setEnabled(true);
        sprintTickButton.setEnabled(false);
    }

    @Override
    public void onClose() {
        pause();
        super.onClose();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        lastMouseHeldPos = e.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (lastMouseHeldPos != null) {
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