package es.sim.game;

import es.sim.Main;
import es.sim.game.board.Board;
import es.sim.gui.*;
import es.sim.io.*;
import es.sim.util.*;

import javax.swing.*;

import java.awt.*;
import java.io.*;

import static es.sim.Main.LOGGER;

/// The main tick loop of the game: Handles ticking {@link Board} with a target count of {@code tps} times per second.<br>
/// Ticking is updating the Board class and advancing a single step into the future
public class TickLoop {
    private final long delayNanos;
    private final Thread loopThread = new Thread(this::loop, "Ticker");
    private final Runnable tickAction;

    private volatile boolean stop = false;
    private volatile boolean pauseRequested = false;
    private volatile boolean paused = false;

    public TickLoop(int tps, Runnable tickAction) {
        this.tickAction = tickAction;
        delayNanos = 1000000000L / tps;
    }

    private void loop() {
        try {
            long lastTickTime = System.nanoTime();

            while (!stop) {
                synchronized (this) {
                    while (paused && !stop) {
                        wait();

                        // Don't try to catch up for the time spent paused.
                        lastTickTime = System.nanoTime();
                    }
                }

                if (stop) {
                    break;
                }

                long now = System.nanoTime();
                long remaining = delayNanos - (now - lastTickTime);

                if (remaining > 0) {
                    long sleepMillis = Util.toMillis(remaining);
                    int sleepNanos = (int) (remaining % 1_000_000L);

                    Thread.sleep(sleepMillis, sleepNanos);
                    continue;
                }

                // If pause was requested while we were waiting for the next tick,
                // pause now. There is no tick currently running that needs to finish.
                synchronized (this) {
                    if (pauseRequested) {
                        paused = true;
                        continue;
                    }
                }

                // Execute the entire tick uninterrupted.
                tickAction.run();

                lastTickTime += delayNanos;

                // If pause() was called during this tick, pause only now,
                // after the tick has completely finished.
                synchronized (this) {
                    if (pauseRequested) {
                        paused = true;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOGGER.error("Exception occured while ticking Board", t);
            Main.getWindow().showScreen(Main.board.parent);

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
        }
    }

    public void start() {
        try {
            loopThread.start();
        } catch(IllegalThreadStateException ignored) {}
    }

    public void stop() {
        stop = true;

        // Wake the thread if it is currently paused.
        synchronized (this) {
            notifyAll();
        }

        loopThread.interrupt();
    }

    public synchronized void pause() {
        pauseRequested = true;
    }

    public synchronized void resume() {
        pauseRequested = false;
        paused = false;
        notifyAll();
    }

    public boolean isPaused() {
        return paused || pauseRequested;
    }

    public static void start(int tps) {
        TickLoop updateLoop = new TickLoop(tps, () -> {
            Main.board.tick();
        });

        Timer renderLoop = new Timer(0, _ -> Main.board.repaint());

        updateLoop.start();
        renderLoop.start();
    }
}