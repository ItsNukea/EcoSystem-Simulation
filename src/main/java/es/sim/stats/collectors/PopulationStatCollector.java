package es.sim.stats.collectors;

import es.sim.*;
import es.sim.game.*;
import es.sim.game.entities.*;
import es.sim.stats.*;
import tools.jackson.databind.node.*;

import java.util.*;

public class PopulationStatCollector<T extends Entity> implements TimeSeriesCollector {
    private final Class<T> entityType;
    private final Simulation owner;
    //The index of the list is the tick the recording happened in. The value of the index is the value of the recording
    private final ArrayList<Long> populationSize = new ArrayList<>();

    public PopulationStatCollector(Class<T> entityType, Simulation sim) {
        this.entityType = entityType;
        populationSize.addFirst(0L);
        this.owner = sim;
    }

    public void addEntry() {
        int i = owner.getCurrentTick();
        populationSize.set(i, populationSize.get(i) + 1);
    }

    public void removeEntry() {
        int i = owner.getCurrentTick();
        populationSize.set(i, populationSize.get(i) - 1);
    }

    public synchronized ArrayList<Long> getData() {
        return populationSize;
    }

    public Class<T> getEntityType() {
        return entityType;
    }

    @Override
    public synchronized void prepareForNextTick(int currentTick) {
        //copy the current number of entities into the next tick
        populationSize.add(currentTick + 1, populationSize.get(currentTick));
    }


    @Override
    public void save(ObjectNode root) {
        root.put("entityType", entityType.getName());
        ArrayNode sizes = root.putArray("data");

        if (populationSize.isEmpty()) return;

        // Format: Run-Length Encoding (RLE)
        long streakValue = populationSize.getFirst();
        int streak = 1;

        for (int i = 1; i < populationSize.size(); i++) {
            long current = populationSize.get(i);
            if (current == streakValue) {
                streak++;
            } else {
                sizes.add(streak);
                sizes.add(streakValue);
                streakValue = current;
                streak = 1;
            }
        }

        // flush the last run
        sizes.add(streak);
        sizes.add(streakValue);
    }

    @Override
    public String getName() {
        return "population/" + entityType.getSimpleName();
    }
}