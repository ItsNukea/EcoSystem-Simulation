package es.sim.stats.collectors;

import es.sim.*;
import es.sim.game.entities.*;
import es.sim.stats.*;
import tools.jackson.databind.node.*;

import java.util.*;

public class SpeedGeneStatCollector<T extends Entity> implements TimeSeriesCollector {
	private final Class<T> entityType;
	// The index of the list is the tick. Each entry stores [sum, count] to calculate average later
	private final ArrayList<double[]> data = new ArrayList<>();
	
	public SpeedGeneStatCollector(Class<T> entityType) {
		this.entityType = entityType;
		data.addFirst(new double[]{0.0, 0.0}); // [sum, count] for tick 0
	}
	
	/**
	 * Adds a speed gene value to the current tick's accumulator
	 */
	public void addSpeedGene(double speedGene) {
		int tick = Main.simulation.getCurrentTick();
		double[] data = this.data.get(tick);
		data[0] += speedGene; // sum
		data[1] += 1;         // count
	}
	
	/**
	 * Gets the average speed gene for the current tick
	 */
	public double getCurrentAverage() {
		int tick = Main.simulation.getCurrentTick();
		double[] data = this.data.get(tick);
		return data[1] == 0 ? Double.NaN : data[0] / data[1];
	}
	
	/**
	 * Gets the average speed gene for a specific tick (for graphing)
	 */
	public double getAverageForTick(int tick) {
		if (tick >= 0 && tick < data.size()) {
			double[] data = this.data.get(tick);
			return data[1] == 0 ? Double.NaN : data[0] / data[1];
		}
		return Double.NaN;
	}
	
	/**
	 * Gets all collected averages (for display/saving)
	 */
	public synchronized ArrayList<Double> getData() {
		ArrayList<Double> averages = new ArrayList<>();
		for (double[] data : data) {
			double avg = data[1] == 0 ? Double.NaN : data[0] / data[1];
			averages.add(avg);
		}
		return averages;
	}
	
	public Class<T> getEntityType() {
		return entityType;
	}
	
	@Override
	public synchronized void prepareForNextTick(int currentTick) {
		// Initialize data for the next tick (copy current tick's data as baseline)
		data.add(currentTick + 1, new double[]{0d, 0d});
	}
	
	@Override
	public void save(ObjectNode root) {
		root.put("entityType", entityType.getName());
		ArrayNode averages = root.putArray("data");
		
		for (double[] data : data) {
			double avg = data[1] == 0 ? Double.NaN : data[0] / data[1];
			if (Double.isFinite(avg)) {
				averages.add(avg);
			} else {
				averages.addNull();
			}
		}
	}
	
	@Override
	public String getName() {
		return "average_speed_gene/" + entityType.getSimpleName();
	}
}