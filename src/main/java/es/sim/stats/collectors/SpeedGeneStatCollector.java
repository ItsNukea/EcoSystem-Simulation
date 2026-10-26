package es.sim.stats.collectors;

import es.sim.*;
import es.sim.game.*;
import es.sim.game.entities.*;
import es.sim.stats.*;
import tools.jackson.databind.node.*;

import java.util.*;

public class SpeedGeneStatCollector<T extends Entity> implements TimeSeriesCollector {
	private final Class<T> entityType;
	private final Simulation owner;
	// The index of the list is the tick. Each entry stores [sum, count] to calculate average later
	private final ArrayList<double[]> data = new ArrayList<>();
	
	public SpeedGeneStatCollector(Class<T> entityType, Simulation sim) {
		this.entityType = entityType;
		this.owner = sim;
		data.addFirst(new double[]{0.0, 0.0}); // [sum, count] for tick 0
	}
	
	/**
	 * Adds a speed gene value to the current tick's accumulator
	 */
	public void addSpeedGene(double speedGene) {
		int tick = owner.getCurrentTick();
		double[] data = this.data.get(tick);
		data[0] += speedGene; // sum
		data[1] += 1;         // count
	}
	
	/**
	 * Gets the average speed gene for the current tick
	 */
	public double getCurrentAverage() {
		int tick = owner.getCurrentTick();
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

		if (data.isEmpty()) return;

		// Format: [count1, value1, count2, value2, ...] where a value of null means "no average this tick"
		double runValue = averageOf(data.getFirst());
		int runLength = 1;

		for (int i = 1; i < data.size(); i++) {
			double current = averageOf(data.get(i));
			// Double.compare treats NaN as equal to NaN, unlike ==
			if (Double.compare(current, runValue) == 0) {
				runLength++;
			} else {
				writeRun(averages, runLength, runValue);
				runValue = current;
				runLength = 1;
			}
		}

		// flush the last run
		writeRun(averages, runLength, runValue);
	}

	private static double averageOf(double[] entry) {
		return entry[1] == 0 ? Double.NaN : entry[0] / entry[1];
	}

	private static void writeRun(ArrayNode array, int length, double value) {
		array.add(length);
		if (Double.isFinite(value)) {
			array.add(value);
		} else {
			array.addNull();
		}
	}
	
	@Override
	public String getName() {
		return "average_speed_gene/" + entityType.getSimpleName();
	}
}