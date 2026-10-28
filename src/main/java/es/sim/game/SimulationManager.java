package es.sim.game;

import es.sim.*;
import es.sim.game.board.*;
import es.sim.game.entities.*;
import es.sim.util.*;

import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;

import static es.sim.Main.*;

public class SimulationManager {
    private static final int TPS = 90;
    private static final int WOLVES_TO_SPAWN = 30;
    private static final int DEER_TO_SPAWN = 500;
    private static final int BUSHES_TO_SPAWN = 325;

    private final int maxConcurrentSimulations;
    private final Consumer<Simulation> crashListener;
    private final CopyOnWriteArrayList<Simulation> simulations = new CopyOnWriteArrayList<>();
    private int nextId = 0;
    private int selected = 0;

    public SimulationManager(int maxConcurrentSimulations, Consumer<Simulation> crashListener) {
        this.maxConcurrentSimulations = maxConcurrentSimulations;
        this.crashListener = crashListener;
    }

    /// Creates and starts new simulations until {@code targetCount} of them are running.
    /// Currently only called once at startup. Restarting stopped simulations later means calling this again
    public synchronized void fillFreeSimulationSlots() {
        for (int running = getRunningCount(); running < maxConcurrentSimulations; running++) {
            addNew();
        }
    }

    public void addNew() {
        Simulation sim = new Simulation(nextId++, TPS, crashListener);
        populate(sim);
        simulations.add(sim);
        sim.start();
        if(DebugVariables.AUTOMATIC_TICK_SPRINTING) {
            sim.sprint(true);
        }
    }

    public void removeSimulation(Simulation sim) {
        simulations.remove(sim);
    }

    public void startAll() {
        LOGGER.info("Starting {} simulations", maxConcurrentSimulations - getRunningCount());
        for(Simulation sim : simulations) sim.start();
    }

    public void stopAll() {
        for(Simulation sim : simulations) sim.stop();
        simulations.clear();
    }

    public int getRunningCount() {
        int running = 0;
        for(Simulation sim : simulations) if(!sim.isStopped()) running++;
        return running;
    }

    public List<Simulation> getSimulations(){
        return List.copyOf(simulations);
    }

    public Simulation getSelected() {
        return simulations.get(selected);
    }

    public void selectNext() {
        if(selected == simulations.size() - 1) {
            selected = 0;
        } else {
            selected++;
        }
        Main.getWindow().showScreen(simulations.get(selected).getBoard());
    }

    public void selectPrevious() {
        if(selected == 0) {
            selected = simulations.size() - 1;
        } else {
            selected--;
        }
        Main.getWindow().showScreen(simulations.get(selected).getBoard());
    }

    public void populate(Simulation sim) {
        Random random = sim.getRandom();

        for (int spawned = 0; spawned < WOLVES_TO_SPAWN; spawned++) {
            int x = random.nextInt(0, sim.getColumns());
            int y = random.nextInt(0, sim.getRows());

            if (sim.getCell(x, y).holder.isEmpty()) {
                Wolf wolf = new Wolf(sim);
                wolf.randomizeAge();
                wolf.setSpeedGene(Util.linearToLogarithmicDistribution(random.nextDouble(), 0.5, 2.0, 1.2));
                sim.registerEntity(wolf, x, y);
                sim.wolfPopulationCollector.addEntry();
                continue;
            }
            spawned--;
        }

        for (int spawned = 0; spawned < DEER_TO_SPAWN; spawned++) {
            int x = random.nextInt(0, sim.getColumns());
            int y = random.nextInt(0, sim.getRows());

            if (sim.getCell(x, y).holder.isEmpty()) {
                Deer deer = new Deer(sim);
                deer.randomizeAge();
                deer.randomizeBreedingCooldown(deer.BREEDING_COOLDOWN_TICKS);
                deer.setSpeedGene(Util.linearToLogarithmicDistribution(random.nextDouble(), 0.5, 2.0, 1.2));
                sim.registerEntity(deer, x, y);
                sim.deerPopulationCollector.addEntry();
                continue;
            }
            spawned--;
        }

        for (int spawned = 0; spawned < BUSHES_TO_SPAWN; spawned++) {
            BerryBush bush = BerryBush.getNewWithRandomProperties(sim);
            if (sim.getCell(bush.getPos().x, bush.getPos().y).holder.isEmpty()) {
                sim.registerEntity(bush, bush.getPos().x, bush.getPos().y);
            } else {
                spawned--;
            }
        }

        sim.setBoard(new Board(Main.TITLE_SCREEN, sim));
    }
}