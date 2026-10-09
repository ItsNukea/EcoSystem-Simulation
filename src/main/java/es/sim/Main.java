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
    public static Board board = null;
    public static TickLoop tickLoop = null;
    public static Timer renderLoop = new Timer(0, _ -> getWindow().repaint());

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
    static void main(String[] args) {
        LOGGER.info("Starting application...");

        //First, do critical flags
        for(String arg : args) {
            if (arg.equals("--devEnv")) {
                DebugVariables.IS_DEVELOPMENT_ENVIRONMENT = true;
                continue;
            }

            if(arg.equals("--showDebugInfo")) {
                DebugVariables.SHOW_ENTITY_DEBUG_INFORMATION = true;
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
            LOGGER.info("Initalizing window");
            window.init();
            window.setVisible(true);
            window.toFront();
            window.requestFocus();
            LOGGER.info("Window initalized");
            window.showScreen(new TitleScreen());

            renderLoop.start();
        });
    }

    public static void resetBoard() {
        board = new Board(window.getScreen());

        if(tickLoop != null) {
            tickLoop.stop();
        }

        Random random = new Random();

        int wolfsToSpawn = 30;

        for(int spawned = 0; spawned < wolfsToSpawn; spawned++) {
            int x = random.nextInt(0, board.getColumns());
            int y = random.nextInt(0, board.getRows());

            if(board.getCell(x, y).holder.isEmpty()) {
                Wolf wolf = new Wolf();
                wolf.randomizeAge();
                wolf.setSpeedGene(Util.linearToLogarithmicDistribution(Math.random(), 0.5, 2.0, 1.5));
                board.registerEntity(wolf, x, y);
                continue;
            }
                spawned--;
            }

            int deerToSpawn = 548;

        for(int spawned = 0; spawned < deerToSpawn; spawned++) {
            int x = random.nextInt(0, board.getColumns());
            int y = random.nextInt(0, board.getRows());

            if(board.getCell(x, y).holder.isEmpty()) {
                Deer deer = new Deer();
                deer.randomizeAge();
                deer.setSpeedGene(Util.linearToLogarithmicDistribution(Math.random(), 0.5, 2.0, 1.5));
                board.registerEntity(deer, x, y);
                continue;
            }
                spawned--;
            }

            int bushesToSpawn = 380;

        for(int spawned = 0; spawned < bushesToSpawn; spawned++) {
            BerryBush bush = BerryBush.getNewWithRandomProperties(board.getBoundsRect());
            if (board.getCell(bush.getPos().x, bush.getPos().y).holder.isEmpty()) {
                board.registerEntity(bush, bush.getPos().x, bush.getPos().y);
            } else {
                spawned--;
            }
        }

        tickLoop = new TickLoop(10, board::tick);
    }

    public static Window getWindow() {
        return window;
    }
}
