
package es.sim.stats.graphs;

import es.sim.game.*;
import es.sim.stats.collectors.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

import java.util.*;

public class SpeedGeneGraph {
    private final ChartPanel panel;
    private final XYSeries deerSeries;
    private final XYSeries wolfSeries;
    private final Simulation owner;
    private int nextTickToPlot = 0;
    
    private final SpeedGeneStatCollector<?> deerCollector;
    private final SpeedGeneStatCollector<?> wolfCollector;
    
    public SpeedGeneGraph(Simulation owner, SpeedGeneStatCollector<?> deerCollector, SpeedGeneStatCollector<?> wolfCollector) {
        this.deerCollector = deerCollector;
        this.wolfCollector = wolfCollector;
        this.owner = owner;
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        deerSeries = new XYSeries("Deer Average SpeedGene");
        wolfSeries = new XYSeries("Wolf Average SpeedGene");
        
        dataset.addSeries(deerSeries);
        dataset.addSeries(wolfSeries);
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Average SpeedGene Over Time",
                "Tick",
                "Average SpeedGene",
                dataset
        );
        
        panel = new ChartPanel(chart);
    }
    
    public ChartPanel getChartPanel() {
        return panel;
    }

    public void updateGraph() {
        int currentTick = owner.getCurrentTick();

        for (int tick = nextTickToPlot; tick <= currentTick; tick++) {
            double deerAverage = deerCollector.getAverageForTick(tick);
            double wolfAverage = wolfCollector.getAverageForTick(tick);

            //notify=false: avoid a repaint event for every single point
            if (Double.isFinite(deerAverage)) {
                deerSeries.add(tick, deerAverage, false);
            }

            if (Double.isFinite(wolfAverage)) {
                wolfSeries.add(tick, wolfAverage, false);
            }
        }

        deerSeries.fireSeriesChanged();
        wolfSeries.fireSeriesChanged();

        nextTickToPlot = currentTick + 1;
    }
}