package es.sim.stats.graphs;

import es.sim.stats.collectors.PopulationStatCollector;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.util.ArrayList;

public class PopulationGraph {
    private final ArrayList<Entry> entries = new ArrayList<>();
    private final ChartPanel panel;
    private final XYSeriesCollection dataset;

    public PopulationGraph(String entityName, PopulationStatCollector<?> collector) {
        dataset = new XYSeriesCollection();

        JFreeChart chart = ChartFactory.createXYLineChart(
                "Population Over Time",
                "Tick",
                "Population",
                dataset
        );

        panel = new ChartPanel(chart);

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

    //FIXME: old data gets corrupted somehow
    public void updateGraph() {
        for (Entry entry : entries) {
            ArrayList<Long> data = entry.collector.getData();

            if (data.isEmpty()) {
                continue;
            }

            int tick = data.size() - 1;
            Long value = data.get(tick);

            if (value == null) {
                continue;
            }

            int itemCount = entry.series.getItemCount();

            if (itemCount > 0 && entry.series.getX(itemCount - 1).intValue() == tick) {
                entry.series.update(itemCount - 1, value);
            } else {
                entry.series.add(tick, value);
            }
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