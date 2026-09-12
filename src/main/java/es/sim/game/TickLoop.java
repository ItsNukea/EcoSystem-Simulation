package es.sim.game;

import es.sim.Main;
import es.sim.game.board.Board;
import es.sim.gui.*;
import es.sim.io.*;

import javax.swing.*;

import java.awt.*;
import java.io.*;

import static es.sim.Main.LOGGER;

/// The main tick loop of the game: Handles ticking {@link Board} with a target count of {@code tps} times per second.<br>
/// Ticking is updating the Board class and advancing a single step into the future
public class TickLoop {
    private final long delayNanos;
    private final Thread loopThread = new Thread(this::loop, "Ticker");
    private boolean stop = false;

    private TickLoop(int tps) {
        delayNanos = 1000000000L / tps;
    }

    private void loop() {
        try {
            long lastTickTime = System.nanoTime();
            while (!stop) {
                long now = System.nanoTime();
                if (now - lastTickTime >= delayNanos) {
                    Main.board.tick();
                    lastTickTime = now;
                }
            }
        } catch (Throwable t) {
            LOGGER.error("Exception occured while ticking Board", t);
            Main.getWindow().showScreen(Main.board.parent);
            try {
                String timestamp = System.getProperty("session.timestamp");
                Desktop.getDesktop().edit(FileManager.getRunDirectory().resolve("logs", timestamp + ".log").toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void start() {
        loopThread.start();
    }

    private void stop() {
        stop = true;
    }

    public static void start(int tps) {
        TickLoop loop = new TickLoop(tps);
        loop.start();
    }
}
