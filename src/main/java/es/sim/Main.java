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

            board.registerEntity(new Wolf(), 0, 0);
            LOGGER.info("Wolf registered, entity count: {}", board.getEntities().size());

            Random random = new Random();
            int deerToSpawn = 4;

            for(int spawned = 0; spawned < deerToSpawn; spawned++) {
                int x = random.nextInt(0, board.getRows());
                int y = random.nextInt(0, board.getColumns());

                if(board.getCell(x, y).holder.isEmpty()) {
                    board.registerEntity(new Deer(), x, y);
                    continue;
                }
                spawned--;
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
