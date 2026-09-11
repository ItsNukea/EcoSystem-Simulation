package es.sim;

import es.sim.game.board.*;
import es.sim.gui.*;
import es.sim.io.*;
import org.slf4j.*;

import javax.swing.*;
import java.text.*;
import java.util.*;

public class Main {
    public static final String DEFAULT_NAMESPACE = "ess";
    public static final Logger LOGGER;
    private static Window window;
    public static Board board;
    private static boolean isDevEnvironment = false;

    static {
        String sessionTimestamp = new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss").format(new Date());
        System.setProperty("session.timestamp", sessionTimestamp);
        LOGGER = LoggerFactory.getLogger("main");
    }

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

            board = new Board(30, 30, window.getScreen());
        });
    }

    public static Window getWindow() {
        return window;
    }

    public static boolean isDevelopmentEnvironment() {
        return isDevEnvironment;
    }
}
