package es.sim;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.game.entities.*;
import es.sim.gui.*;
import es.sim.io.*;
import es.sim.util.*;
import org.slf4j.*;

import javax.swing.*;
import javax.swing.Timer;
import java.text.*;
import java.util.*;

public class Main {
    public static final Logger LOGGER;
    private static Window window;
    public static Simulation simulation;
    public static Board board = null;
    public static Timer renderLoop = new Timer(4, _ -> getWindow().repaint());

    static {
        String sessionTimestamp = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss").format(new Date());
        System.setProperty("session.timestamp", sessionTimestamp);
        LOGGER = LoggerFactory.getLogger("main");
    }

    /// The main entrypoint of the program, responsible for resolving runtime arguments, generating files, and initializing the window
    /// @param args
    ///     - {@code --devEnv}: Used to signify that the program is running in an IDE.<br>**<span style="color:red">THIS FLAG SHOULD BE ENABLED IF AND ONLY IF THE PROGRAM RUNS IN AN IDE!</span>**
    ///     - {@code --showDebugInfo}: Shows to which square every entity is path finding to.
    ///     - {@code --clearLogFiles}: Clears all log files except {@code latest.log} in the log directory
    ///     - {@code --limitTickLength}: Does not let a simulation simulate forever, but ends it when hitting a certain tick count
    static void main(String[] args) {
        LOGGER.info("Starting application...");

        //First, do critical flags
        for(String arg : args) {
            switch(arg) {
                case "--devEnv" -> DebugVariables.IS_DEVELOPMENT_ENVIRONMENT = true;
                case "--showDebugInfo" -> DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION = true;
                case "--limitTickLength" -> DebugVariables.LIMIT_SIMULATION_TICK_LENGTH = true;
            }
        }

        for(String arg : args) {
            if(arg.equals("--clearLogFiles")) {
                FileManager.clearLogs();
            }
        }

        FileManager.generateFiles();

        SwingUtilities.invokeLater(() -> {
            window = new Window();
            LOGGER.info("Initializing window");
            window.init();
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
            window.setFullScreen();
            LOGGER.info("Window initialized");
            window.showScreen(new TitleScreen());

            renderLoop.start();
        });
    }

    public static void resetBoard() {
        if (simulation != null) simulation.stop();

        simulation = new Simulation(90);
        board = new Board(window.getScreen(), simulation);

        Random random = simulation.getRandom();

        int wolfsToSpawn = 30;
        for (int spawned = 0; spawned < wolfsToSpawn; spawned++) {
            int x = random.nextInt(0, simulation.getColumns());
            int y = random.nextInt(0, simulation.getRows());

            if (simulation.getCell(x, y).holder.isEmpty()) {
                Wolf wolf = new Wolf();
                wolf.randomizeAge();
                wolf.setSpeedGene(Util.linearToLogarithmicDistribution(random.nextDouble(), 0.5, 2.0, 1.5));
                simulation.registerEntity(wolf, x, y);
                simulation.wolfPopulationCollector.addEntry();
                continue;
            }
            spawned--;
        }

        int deerToSpawn = 500;
        for (int spawned = 0; spawned < deerToSpawn; spawned++) {
            int x = random.nextInt(0, simulation.getColumns());
            int y = random.nextInt(0, simulation.getRows());

            if (simulation.getCell(x, y).holder.isEmpty()) {
                Deer deer = new Deer();
                deer.randomizeAge();
                deer.randomizeBreedingCooldown(Deer.BREEDING_COOLDOWN_TICKS);
                deer.setSpeedGene(Util.linearToLogarithmicDistribution(random.nextDouble(), 0.5, 2.0, 1.5));
                simulation.registerEntity(deer, x, y);
                simulation.deerPopulationCollector.addEntry();
                continue;
            }
            spawned--;
        }

        int bushesToSpawn = 325;
        for (int spawned = 0; spawned < bushesToSpawn; spawned++) {
            BerryBush bush = BerryBush.getNewWithRandomProperties(simulation);
            if (simulation.getCell(bush.getPos().x, bush.getPos().y).holder.isEmpty()) {
                simulation.registerEntity(bush, bush.getPos().x, bush.getPos().y);
            } else {
                spawned--;
            }
        }
    }

    public static Window getWindow() {
        return window;
    }
}