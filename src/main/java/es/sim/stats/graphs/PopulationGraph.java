package es.sim.stats.graphs;

import es.sim.*;
import es.sim.game.*;
import es.sim.stats.collectors.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

import java.util.*;

public class PopulationGraph {
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final ChartPanel panel;
    private final XYSeriesCollection dataset;
    private final Simulation owner;

    public PopulationGraph(String entityName, PopulationStatCollector<?> collector, Simulation owner) {
        this.dataset = new XYSeriesCollection();
        this.owner = owner;

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Population Over Time",
                "Tick",
                "Population",
                dataset
        );

        this.panel = new ChartPanel(chart);

        addCollector(entityName, collector);
    }

    public void addCollector(String entityName, PopulationStatCollector<?> collector) {
        Entry entry = new Entry(entityName, collector);

        entries.add(entry);
        dataset.addSeries(entry.series);
    }

    public ChartPanel getChartPanel() {
        return panel;
    }

    public void updateGraph() {
        for (Entry entry : entries) {
            ArrayList<Long> data = entry.collector.getData();

            if (data.isEmpty()) {
                continue;
            }

            int tick = owner.getCurrentTick();
            Long value = data.get(tick);

            entry.series.add(tick, value);
        }
    }

    private class Entry {
        private final XYSeries series;
        private final PopulationStatCollector<?> collector;

        private Entry(String entityName, PopulationStatCollector<?> collector) {
            this.collector = collector;
            this.series = new XYSeries(entityName + " Population");
        }
    }
}