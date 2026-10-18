
package es.sim.stats.graphs;

import es.sim.Main;
import es.sim.game.board.Board;
import es.sim.game.entities.*;
import org.jfree.chart.*;
import org.jfree.data.xy.*;

public class SpeedGeneGraph {

    private final ChartPanel panel;
    private final XYSeries deerSeries;
    private final XYSeries wolfSeries;

    public SpeedGeneGraph() {
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

    public void updateGraph(double deerSum, int deerCount, double wolfSum, int wolfCount) {
        Board board = Main.board;
        int tick = board.getCurrentTick();
        
        double deerAverage = deerSum / deerCount;
        double wolfAverage = wolfSum / wolfCount;

        if (Double.isFinite(deerAverage)) {
            deerSeries.add(tick, deerAverage);
        }

        if (Double.isFinite(wolfAverage)) {
            wolfSeries.add(tick, wolfAverage);
        }
    }

    private double calculateAverage(Board board, Class<? extends Entity> entityType) {
        double total = 0;
        int count = 0;

        for (Entity entity : board.getEntities()) {
            if (entityType.isInstance(entity)) {
                if (entity instanceof Deer deer) {
                    total += deer.getSpeedGene();
                    count++;
                } else if (entity instanceof Wolf wolf) {
                    total += wolf.getSpeedGene();
                    count++;
                }
            }
        }

        return count == 0
                ? Double.NaN
                : total / count;
    }
}