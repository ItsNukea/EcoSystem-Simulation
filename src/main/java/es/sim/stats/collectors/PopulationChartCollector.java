package es.sim.stats.collectors;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.stats.*;

import java.util.*;

public class PopulationChartCollector<T extends Entity> implements StatCollector {
    private final Class<T> populationType;
    private ArrayList<Long> populationSize = new ArrayList<>();

    public PopulationChartCollector(Class<T> populationType) {
        this.populationType = populationType;
    }

    public void addEntry() {
        int i = Main.board.getCurrentTick();
        populationSize.add(i, populationSize.get(i) + 1);
    }

    public void removeEntry() {
        int i = Main.board.getCurrentTick();
        populationSize.add(i, populationSize.get(i) + 1);
    }
}
