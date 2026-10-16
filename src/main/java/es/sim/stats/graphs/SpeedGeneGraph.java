
package es.sim.stats.graphs;

import es.sim.*;
import es.sim.game.board.Board;
import es.sim.game.entities.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

    public void updateGraph() {
        Board board = Main.board;
        int tick = board.getCurrentTick();

        double deerAverage = calculateAverage(
                board, Deer.class
        );

        double wolfAverage = calculateAverage(
                board, Wolf.class
        );

        if (!Double.isNaN(deerAverage)) {
            deerSeries.add(tick, deerAverage);
        }

        if (!Double.isNaN(wolfAverage)) {
            wolfSeries.add(tick, wolfAverage);
        }
    }

    private double calculateAverage(
            Board board,
            Class<? extends Entity> entityType
    ) {
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
