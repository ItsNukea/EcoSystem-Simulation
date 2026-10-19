
package es.sim.io;

import es.sim.game.board.*;
import es.sim.stats.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public class StatisticsSaver {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final ObjectWriter WRITER = MAPPER.writerWithDefaultPrettyPrinter();
	
	public static synchronized void saveSimulationStatistics(Board board) {
		Path savePath = getSavePath();

		ObjectNode root = MAPPER.createObjectNode();
		ArrayList<StatCollector> stats = board.getStatistics().collectors;

		ObjectNode metadata = root.putObject("meta");
		metadata.put("seed", board.getRandomSeed());
		metadata.put("last_tick", board.getCurrentTick());
		ObjectNode statisticsNode = root.putObject("statistics");

		for(StatCollector collector : stats) {
			collector.save(statisticsNode.putObject(collector.getName()));
		}

		FileManager.getRunDirectory().resolve("saves").toFile().mkdirs();
		WRITER.writeValue(savePath, root);
	}

	private static Path getSavePath() {
		LocalDateTime currentTime = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

		while(true) {
			Path path = FileManager.getRunDirectory().resolve("saves", formatter.format(currentTime) + ".json");
			if(!path.toFile().exists()) {
				return path;
			}
			currentTime.plusSeconds(1);
		}
	}
}