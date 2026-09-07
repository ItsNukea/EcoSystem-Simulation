package es.sim;

import es.sim.gui.*;
import org.slf4j.*;
import org.slf4j.Logger;

import javax.swing.*;
import java.util.*;

public class Main {
    public static final String DEFAULT_NAMESPACE = "ess";
    public static final Logger LOGGER = LoggerFactory.getLogger("main");
    private static Window window;

    static void main(String[] args) {
        LOGGER.info("Starting application...");

        SwingUtilities.invokeLater(() -> {
            window = new Window();
            LOGGER.info("Initalizing window");
            window.init();
            window.setVisible(true);
            LOGGER.info("Window initalized");
            window.showScreen(new TitleScreen());
        });
        //TODO: do something with String[] args
    }

    public static Window getWindow() {
        return window;
    }
}
