package es.sim.game;

import es.sim.*;
import es.sim.game.board.*;
import es.sim.io.*;
import es.sim.util.*;

import java.awt.*;
import java.io.*;
import java.util.function.*;

import static es.sim.Main.*;

/// The main tick loop of the game: Handles ticking {@link Board} with a target count of {@code tps} times per second.<br>
/// Ticking is updating the Board class and advancing a single step into the future
public class TickLoop {
    private final long delayNanos;
    private final Thread loopThread;
    private final Consumer<Throwable> crashHandler;
    private final Runnable tickAction;

    private volatile boolean stop = false;
    private volatile boolean pauseRequested = false;
    private volatile boolean paused = false;
    private volatile boolean sprinting = false;

    public TickLoop(String name, int tps, Runnable tickAction, Consumer<Throwable> crashHandler) {
        this.tickAction = tickAction;
        this.crashHandler = crashHandler;
        this.delayNanos = 1000000000L / tps;
        this.loopThread = new Thread(this::loop, name);
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

                if (remaining > 0 && !sprinting) {
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

                if (sprinting) {
                    lastTickTime = System.nanoTime();
                } else {
                    lastTickTime += delayNanos;
                }

                // If pause() was called during this tick, pause only now,
                // after the tick has completely finished.
                synchronized (this) {
                    if (pauseRequested) {
                        paused = true;
                    }
                }
            }
        } catch (InterruptedException e) {
            if(stop) {
                LOGGER.info("Stopping Ticker Thread permanently");
            } else {
                LOGGER.error("Ticker Thread interrupted, but not stopped");
            }
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            LOGGER.error("Exception occurred while ticking ({})", Thread.currentThread().getName(), t);
            stop = true;
            crashHandler.accept(t);
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
    public boolean isStopped() {
        return stop;
    }

    public boolean isSprinting() {
        return this.sprinting;
    }

    public void sprint(boolean sprint) {
        this.sprinting = sprint;
    }

    public long getTPS() {
        return 1000000000 / delayNanos;
    }
}