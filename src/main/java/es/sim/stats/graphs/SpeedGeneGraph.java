
package es.sim.stats.graphs;

import es.sim.*;
import es.sim.stats.collectors.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

public class SpeedGeneGraph {
    private final ChartPanel panel;
    private final XYSeries deerSeries;
    private final XYSeries wolfSeries;
    
    private final SpeedGeneStatCollector<?> deerCollector;
    private final SpeedGeneStatCollector<?> wolfCollector;
    
    public SpeedGeneGraph(SpeedGeneStatCollector<?> deerCollector, SpeedGeneStatCollector<?> wolfCollector) {
        this.deerCollector = deerCollector;
        this.wolfCollector = wolfCollector;
        
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
        int tick = Main.simulation.getCurrentTick();
        
        double deerAverage = deerCollector.getAverageForTick(tick);
        double wolfAverage = wolfCollector.getAverageForTick(tick);
        
        if (Double.isFinite(deerAverage)) {
            deerSeries.add(tick, deerAverage);
        }
        
        if (Double.isFinite(wolfAverage)) {
            wolfSeries.add(tick, wolfAverage);
        }
    }
}