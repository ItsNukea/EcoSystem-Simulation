package es.sim.stats.graphs;

import es.sim.game.entities.*;
import es.sim.stats.collectors.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

import java.util.*;

public class PopulationGraph {
    public final XYSeries series;
    public final PopulationStatCollector<?> collector;
    private final ChartPanel panel;

    public PopulationGraph(String entityName, PopulationStatCollector<?> collector) {
        this.collector = collector;
        this.series = new XYSeries(entityName + " Population");

        // Populate the initial history.
        ArrayList<Long> data = collector.getData();
        for (int tick = 0; tick < data.size(); tick++) {
            series.add(tick, data.get(tick));
        }

        XYSeriesCollection dataset = new XYSeriesCollection(series);

        JFreeChart chart = ChartFactory.createXYLineChart(
                entityName + " Population Over Time",
                "Tick",
                "Population",
                dataset
        );

        panel = new ChartPanel(chart);
    }

    public ChartPanel getChartPanel() {
        return panel;
    }

    public void updateGraph() {
        ArrayList<Long> data = collector.getData();

        for (int tick = series.getItemCount(); tick < data.size(); tick++) {
            series.add(tick, data.get(tick));
        }
    }
}
