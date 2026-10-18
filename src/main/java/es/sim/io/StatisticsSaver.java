
package es.sim.io;

import es.sim.game.board.Board;
import es.sim.stats.StatCollector;

import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static es.sim.Main.LOGGER;

public class StatisticsSaver {
	
	public static void saveSimulationStatistics(Board board) {
		
		
		try {
			String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
			String filename = "simulation_" + timestamp + ".txt";
			Path savePath = FileManager.getRunDirectory().resolve("saves").resolve(filename);
			
			StringBuilder stats = new StringBuilder();
			stats.append("=== Ecosystem Simulation Statistics ===\n");
			stats.append("Timestamp: ").append(new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date())).append("\n");
			stats.append("Final Tick: ").append(board.getCurrentTick()).append("\n\n");
			
			// Collect all statistics
			for(StatCollector collector : board.getStatistics().collectors) {
				stats.append(collector.getClass().getSimpleName()).append(":\n");
				stats.append(collector).append("\n\n");
			}
			
			// Write to file
			Files.writeString(savePath, stats.toString());
			LOGGER.info("Simulation statistics saved to: {}", savePath.toAbsolutePath());
			
		} catch(IOException e) {
			LOGGER.error("Failed to save simulation statistics", e);
		}
	}
}