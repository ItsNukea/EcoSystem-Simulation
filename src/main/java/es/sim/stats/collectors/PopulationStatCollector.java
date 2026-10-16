package es.sim.stats.collectors;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.stats.*;

import java.sql.*;
import java.util.*;

public class PopulationStatCollector<T extends Entity> implements TimeSeriesCollector {
    private final Class<T> populationType;
    //The index of the list is the tick the recording happened in. The value of the index is the value of the recording
    private final ArrayList<Long> populationSize = new ArrayList<>();

    public PopulationStatCollector(Class<T> populationType) {
        this.populationType = populationType;
        populationSize.addFirst(0L);
    }

    public void addEntry() {
        int i = Main.board.getCurrentTick();
        populationSize.set(i, populationSize.get(i) + 1);
    }

    public void removeEntry() {
        int i = Main.board.getCurrentTick();
        populationSize.set(i, populationSize.get(i) - 1);
    }

    public synchronized ArrayList<Long> getData() {
        return populationSize;
    }

    @Override
    public synchronized void prepareForNextTick(int currentTick) {
        //copy the current number of entities into the next tick
        populationSize.add(currentTick + 1, populationSize.get(currentTick));
    }
}
