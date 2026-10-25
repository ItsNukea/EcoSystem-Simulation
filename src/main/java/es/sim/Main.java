package es.sim;

import es.sim.game.*;
import es.sim.game.board.*;
import es.sim.gui.*;
import es.sim.io.*;
import es.sim.util.*;
import org.slf4j.*;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.*;
import java.text.*;
import java.util.*;

public class Main {
    public static final Logger LOGGER;
    private static Window window;
    public static SimulationManager manager;
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

    /// Stops any running simulations, starts {@code simulationCount} fresh ones in parallel, and creates a
    /// {@link Board} for the first one, which is the one the user watches
    public static void startSimulations() {
        if (manager != null) manager.stopAll();

        // Change this variable to determine how many simulations should run in parallel
        int simulationCount = 2;
        manager = new SimulationManager(simulationCount, Main::onSimulationCrashed);
        manager.fillFreeSimulationSlots();
        manager.startAll();
    }

    private static void onSimulationCrashed(Simulation crashed) {
        // Runs on the crashed simulation's ticker thread; TickLoop already logged the exception
        SwingUtilities.invokeLater(() -> {
            // A simulation nobody is watching just stops. Only the watched one sends the user back to the title screen

            getWindow().showScreen(crashed.getBoard().parent);

            String timestamp = System.getProperty("session.timestamp");
            File logFile = FileManager.getRunDirectory().resolve("logs", timestamp + ".log").toFile();
            LOGGER.info("Full crash details written to: {}", logFile.getAbsolutePath());

            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(logFile);
                }
            } catch (IOException e) {
                LOGGER.warn("Could not automatically open the log file. You can open it manually at: {}", logFile.getAbsolutePath());
            }
        });
    }

    public static Window getWindow() {
        return window;
    }
}