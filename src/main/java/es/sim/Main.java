package es.sim;

import es.sim.game.board.*;
import es.sim.game.entities.*;
import es.sim.gui.*;
import es.sim.io.*;
import org.slf4j.*;

import javax.swing.*;
import java.awt.*;
import java.text.*;
import java.util.*;

public class Main {
    public static final Logger LOGGER;
    private static Window window;
    public static Board board;
    private static boolean isDevEnvironment = false;

    static {
        String sessionTimestamp = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss").format(new Date());
        System.setProperty("session.timestamp", sessionTimestamp);
        LOGGER = LoggerFactory.getLogger("main");
    }

    ///The main entrypoint of the program, responsible for resolving runtime arguments, generating files, and initializing the window
    static void main(String[] args) {
        LOGGER.info("Starting application...");

        //TODO: do something with String[] args

        //First, do critical flags
        for(String arg : args) {
            if (arg.equals("--devEnv")) {
                isDevEnvironment = true;
                break;
            }
        }

        for(String arg : args) {
            if(arg.equals("-clearLogFiles")) {
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

            board.registerEntity(new Wolf(), 30, 30);
            LOGGER.info("Wolf registered, entity count: {}", board.getEntities().size());

            Random random = new Random();
            Point spawnCenter = new Point(10, 10);
            int deerToSpawn = 4;
            int spawned = 0;

            while (spawned < deerToSpawn) {
                int x = spawnCenter.x + random.nextInt(5) - 2; //spawnCenter.x - 2 .. spawnCenter.x + 2
                int y = spawnCenter.y + random.nextInt(5) - 2;

                if (board.getCell(x, y).holder.isEmpty()) {
                    board.registerEntity(new Deer(), x, y);
                    spawned++;
                }
            }
        });
    }

    public static Window getWindow() {
        return window;
    }

    public static boolean isDevelopmentEnvironment() {
        return isDevEnvironment;
    }
}
