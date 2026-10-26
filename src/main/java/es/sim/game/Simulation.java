package es.sim.game;

import es.sim.game.board.*;
import es.sim.game.entities.*;
import es.sim.io.*;
import es.sim.stats.*;
import es.sim.stats.collectors.*;
import es.sim.util.*;
import org.jfree.data.statistics.*;

import java.awt.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static es.sim.Main.*;

/// One run of the simulation. Owns the world (grid, entities, random), the tick counter, the statistics and the
/// {@link TickLoop}. It knows nothing about rendering: views like the Board subscribe through {@link #addTickListener}
public class Simulation {
    public static final int DEFAULT_ROWS = 100;
    public static final int DEFAULT_COLUMNS = 100;

    // World
    public final int SIMULATION_TICK_LIMIT = 30000;
    private final Cell[][] grid;
    private final CopyOnWriteArrayList<Entity> entities = new CopyOnWriteArrayList<>();
    private final HashMap<Identifier, Integer> entityCounts = new HashMap<>();
    private final int rows;
    private final int columns;
    private final long seed;
    private final Random random = new Random();
    private final int id;
    private final Consumer<Simulation> crashListener;
    private Board board;

    // Statistics
    public final PopulationStatCollector<Wolf> wolfPopulationCollector;
    public final PopulationStatCollector<Deer> deerPopulationCollector;
    public final DeathCauseStatCollector<Wolf> wolfDeathCauseCollector;
    public final DeathCauseStatCollector<Deer> deerDeathCauseCollector;
    public final SpeedGeneStatCollector<Wolf> wolfSpeedGeneCollector;
    public final SpeedGeneStatCollector<Deer> deerSpeedGeneCollector;
    private final StatisticManager statisticManager = new StatisticManager();

    // Time
    private final TickLoop tickLoop;
    private final CopyOnWriteArrayList<Runnable> tickListeners = new CopyOnWriteArrayList<>();
    private int currentTick = 0;

    public Simulation(int id, int tps, Consumer<Simulation> crashListener) {
        this(id, tps, DEFAULT_ROWS, DEFAULT_COLUMNS, crashListener);
    }

    public Simulation(int id, int tps, int rows, int columns, Consumer<Simulation> crashListener) {
        this.rows = rows;
        this.columns = columns;
        this.crashListener = crashListener;
        this.id = id;


        grid = new Cell[columns][rows];

        for(int x = 0; x < columns; x++) {
            for(int y = 0; y < rows; y++) {
                grid[x][y] = new Cell();
            }
        }

        //I hate that this is the only way to get a random seed
        seed = random.nextLong();
        random.setSeed(seed);

        wolfPopulationCollector = new PopulationStatCollector<>(Wolf.class, this);
        deerPopulationCollector = new PopulationStatCollector<>(Deer.class, this);
        wolfDeathCauseCollector = new DeathCauseStatCollector<>(Wolf.class);
        deerDeathCauseCollector = new DeathCauseStatCollector<>(Deer.class);
        wolfSpeedGeneCollector = new SpeedGeneStatCollector<>(Wolf.class, this);
        deerSpeedGeneCollector = new SpeedGeneStatCollector<>(Deer.class, this);


        statisticManager.addAllCollectors(
                wolfPopulationCollector,
                deerPopulationCollector,
                wolfDeathCauseCollector,
                deerDeathCauseCollector,
                wolfSpeedGeneCollector,
                deerSpeedGeneCollector
        );

        tickLoop = new TickLoop("Simulation Ticker #" + id, tps, this::tick, t -> {
            LOGGER.error("Simulation Crashed", t);
            crashListener.accept(this);
        });
    }

    // ---------------------------------------------------------------- ticking

    /// Ticks every registered entity and makes them advance 1 step into the future
    public void tick() {
        //Check end conditions before ticking
        if(shouldEndSimulation()) {
            stop();
            return;
        }

        for(Entity entity : entities) {
            //Removed earlier this tick (e.g. eaten by a wolf), so it must not act anymore
            if (!entities.contains(entity)) continue;
            entity.tick();

            if (entity instanceof Wolf wolf) {
                wolfSpeedGeneCollector.addSpeedGene(wolf.getSpeedGene());
            } else if (entity instanceof Deer deer) {
                deerSpeedGeneCollector.addSpeedGene(deer.getSpeedGene());
            }
        }

        for(Runnable listener : tickListeners) {
            listener.run();
        }

        for(StatCollector collector : statisticManager.collectors) {
            if(collector instanceof TimeSeriesCollector tsCollector) tsCollector.prepareForNextTick(currentTick);
        }

        currentTick++;
    }

    private boolean shouldEndSimulation() {
        if(getEntityCount(Identifier.of("entity:wolf")) == 0) {
            LOGGER.info("Simulation ended: No more wolves");
            return true;
        }

        if(currentTick >= SIMULATION_TICK_LIMIT && DebugVariables.LIMIT_SIMULATION_TICK_LENGTH) {
            LOGGER.info("Simulation ended: Hit tick length limit");
            return true;
        }

        return false;
    }

    /// Registers a callback that runs after every tick's entity updates, before the tick counter advances
    public void addTickListener(Runnable listener) {
        tickListeners.add(listener);
    }

    // ------------------------------------------------------------------ world

    /// Gets a cell at a specified coordinate {@code (x, y)}
    public Cell getCell(int x, int y) {
        if(x < 0 || x >= columns || y < 0 || y >= rows) {
            String message = String.format("Could not provide Cell at coordinate (%d, %d) because the coordinate does not exist", x, y);
            LOGGER.error(message);
            throw new IllegalArgumentException(message);
        }

        return grid[x][y];
    }

    public int getId() {
        return id;
    }

    /// Registers an entity so that it can be ticked and rendered
    ///     @param x The x-coordinate of the cell the entity should be registered on
    ///     @param y The y-coordinate of the cell the entity should be registered on
    public void registerEntity(Entity e, int x, int y) {
        e.setPos(x, y);
        entities.add(e);
        grid[x][y].setContents(e);
        entityCounts.merge(e.getEntityID(), 1, Integer::sum);
    }

    /// Unregisters an entity to stop ticking and rendering it. This will mostly be used when an entity dies
    public void unregisterEntity(Entity e) {
        entities.remove(e);
        entityCounts.merge(e.getEntityID(), -1, Integer::sum);

        Cell cell = getCell(e.getPos().x, e.getPos().y);
        if(cell.holder.isPresent() && cell.holder.get() == e) {
            cell.setContents(null);
        }
    }

    public CopyOnWriteArrayList<Entity> getEntities() {
        return entities;
    }
    public int getEntityCount(Identifier id) {
        return entityCounts.getOrDefault(id, 0);
    }
    public int getRows() {
        return rows;
    }
    public int getColumns() {
        return columns;
    }
    public Board getBoard() {
        return board;
    }
    public void setBoard(Board board) {
        this.board = board;
    }

    /// Returns the bounds of the grid as a rectangle with a width and height, with coordinate (0, 0)
    public Rectangle getBoundsRect() { return new Rectangle(0, 0, columns, rows); }

    public Random getRandom() { return random; }
    public long getSeed() { return seed; }

    // ------------------------------------------------------------- statistics

    public StatisticManager getStatistics() { return statisticManager; }
    public int getCurrentTick() { return currentTick; }

    public DeathCauseStatCollector<?> getDeathCauseCollector(Class<? extends Entity> type) {
        if (type == Wolf.class) return wolfDeathCauseCollector;
        if (type == Deer.class) return deerDeathCauseCollector;
        return null;
    }

    // ---------------------------------------------------------- loop control

    public void start() {
        tickLoop.start();
        LOGGER.info("Starting simulation #{}", id);
    }
    public void stop() {
        tickLoop.stop();
        StatisticsSaver.saveSimulationStatistics(this);
        manager.addNew();
        manager.removeSimulation(this);
    }
    public void pause() {
        tickLoop.pause();
        LOGGER.info("Pausing Simulation #{}", id);
    }
    public void resume() {
        tickLoop.resume();
        LOGGER.info("Resuming Simulation #{}", id);
    }
    public boolean isPaused() {
        return tickLoop.isPaused();
    }
    public boolean isSprinting() {
        return tickLoop.isSprinting();
    }
    public boolean isStopped() {
        return tickLoop.isStopped();
    }
    public void sprint(boolean sprint) {
        tickLoop.sprint(sprint);
    }

    public TickLoop getTickLoop() {
        return tickLoop;
    }
}