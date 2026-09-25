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
    public static Board board;
    public static TickLoop loop;
    public static Timer renderLoop = new Timer(0, _ -> Main.board.repaint());

    static {
        String sessionTimestamp = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss").format(new Date());
        System.setProperty("session.timestamp", sessionTimestamp);
        LOGGER = LoggerFactory.getLogger("main");
    }

    /// The main entrypoint of the program, responsible for resolving runtime arguments, generating files, and initializing the window
    /// @param args
    ///     - {@code --devEnv}: Used to signify that the program is running in an IDE.<br>**<span style="color:red">THIS FLAG SHOULD BE ENABLED IF AND ONLY IF THE PROGRAM RUNS IN AN IDE!</span>**
    ///     - {@code --showPathFindingTarget}: Shows to which square every entity is path finding to.
    ///     - {@code --clearLogFiles}: Clears all log files except {@code latest.log} in the log directory
    static void main(String[] args) {
        LOGGER.info("Starting application...");

        //First, do critical flags
        for(String arg : args) {
            if (arg.equals("--devEnv")) {
                DebugVariables.IS_DEVELOPMENT_ENVIRONMENT = true;
                continue;
            }

            if(arg.equals("--showPathFindingTarget")) {
                DebugVariables.SHOW_ENTITY_PATHFINDING_TARGET = true;
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

            board = new Board(Board.DEFAULT_ROWS, Board.DEFAULT_COLUMNS, window.getScreen());

            Random random = new Random();

            int wolfsToSpawn = 1;

            for(int spawned = 0; spawned < wolfsToSpawn; spawned++) {
                int x = random.nextInt(0, board.getColumns());
                int y = random.nextInt(0, board.getRows());

                if(board.getCell(x, y).holder.isEmpty()) {
                    board.registerEntity(new Wolf(), x, y);
                    continue;
                }
                spawned--;
            }

            int deerToSpawn = 5;

            for(int spawned = 0; spawned < deerToSpawn; spawned++) {
                int x = random.nextInt(0, board.getColumns());
                int y = random.nextInt(0, board.getRows());

                if(board.getCell(x, y).holder.isEmpty()) {
                    board.registerEntity(new Deer(), x, y);
                    continue;
                }
                spawned--;
            }

            loop = new TickLoop(2, board::tick);
            renderLoop.start();
        });
    }

    public static Window getWindow() {
        return window;
    }
}
