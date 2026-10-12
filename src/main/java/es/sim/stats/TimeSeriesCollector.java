package es.sim.stats;

public interface TimeSeriesCollector extends StatCollector{
    void prepareForNextTick(int currentTick);
}
