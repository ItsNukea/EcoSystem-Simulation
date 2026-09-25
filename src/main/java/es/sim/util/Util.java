package es.sim.util;

public class Util {
    public static long toNanos(long millis) {
        return millis * 1000000L;
    }

    public static long toMillis(long nanos) {
        return nanos / 1000000L;
    }
}
