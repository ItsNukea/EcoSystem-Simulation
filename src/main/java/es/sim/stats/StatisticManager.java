package es.sim.stats;

import java.util.*;

public class StatisticManager {
    public ArrayList<StatCollector> collectors = new ArrayList<>();

    public StatisticManager() {}

    public void addAllCollectors(StatCollector... collectors) {
        for(StatCollector collector : collectors) addCollector(collector);
    }

    public void addCollector(StatCollector collector) {
        collectors.add(collector);
    }
}
