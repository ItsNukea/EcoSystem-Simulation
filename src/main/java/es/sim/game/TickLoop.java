package es.sim.game;

import es.sim.Main;

public class TickLoop {
    private final long delayNanos;
    private final Thread loopThread = new Thread(this::loop);
    private boolean stop = false;

    private TickLoop(int tps) {
        delayNanos = 1000000000L / tps;
    }

    private void loop() {
        long lastTickTime = System.nanoTime();

        while (!stop) {
            long now = System.nanoTime();
            if (now - lastTickTime >= delayNanos) {
                Main.board.tick();
                lastTickTime = now;
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
