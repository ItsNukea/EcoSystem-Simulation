package es.sim.io;

import tools.jackson.databind.*;
import tools.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;

import static es.sim.Main.LOGGER;

public class JsonCompressor {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final ObjectWriter WRITER = MAPPER.writer();
	
	public static void compressSaves() {
		LOGGER.info("Starting Compressing of Save Files...");
		
		
		Path saves = FileManager.getRunDirectory().resolve("saves");
		Path compressedSaves = FileManager.getRunDirectory().resolve("compressed");
		compressedSaves.toFile().mkdirs();
		
		
		try(
				DirectoryStream<Path> stream = Files.newDirectoryStream(saves);
				ExecutorService executor = Executors.newFixedThreadPool(12)
		) {
			for(Path path : stream) {
				executor.submit(() -> {
					try {
						LOGGER.info("Compressing {}", path.toString());
						Path output = compressedSaves.resolve(path.getFileName() + ".gz");
						
						OutputStream outStream = Files.newOutputStream(output);
						GZIPOutputStream gzip = new GZIPOutputStream(outStream);
						
						ObjectNode root = MAPPER.readValue(path, ObjectNode.class);
						WRITER.writeValue(gzip, root);
						
						outStream.close();
						gzip.close();
					} catch(IOException e) {
						executor.shutdownNow();
						throw new UncheckedIOException(e);
					}
				});
			}
			
			executor.shutdown();
			//wait until the executor is terminated, don't care about timeout
			while(!executor.isTerminated()) {}
		} catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		
		System.exit(0);
	}
}