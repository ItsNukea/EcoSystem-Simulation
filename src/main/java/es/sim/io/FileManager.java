package es.sim.io;

import es.sim.*;

import java.io.*;
import java.nio.file.*;

import static es.sim.Main.LOGGER;

/// This class has some helper methods related to IO
@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {
    ///Generates the files needed to put the save data for the program in
    public static void generateFiles() {
        try {
            File runDir = getRunDirectoryFile();
            if (!runDir.exists()) {
                runDir.mkdir();
                runDir.toPath().resolve("logs").toFile().createNewFile();
                runDir.toPath().resolve("config").toFile().createNewFile();
                runDir.toPath().resolve("saves").toFile().createNewFile();
            }
        } catch(IOException e) {
            LOGGER.error("Failed to create file", e);
            throw new UncheckedIOException(e);
        }
    }

    ///Clears all logs in the logs folder, except for {@code latest.log}
    public static void clearLogs() {
        File logsDirectory = getRunDirectory().resolve("logs").toFile();
        for(File log : logsDirectory.listFiles()) {
            if(!log.getName().equals("latest.log")) {
                log.delete();
            }
        }
    }

    ///Returns the Run Directory as the program. Useful to determine where the save files should be stored
    public static Path getRunDirectory() {
        Path runDir;
        if(Main.isDevelopmentEnvironment()) {
            runDir = Paths.get("");
        } else {
            Path userHome = Paths.get(System.getProperty("user.home"));
            runDir = userHome.resolve("AppData", "Roaming", "EcoSystemSimulation");
        }
        return runDir;
    }

    ///Does the same as {@link FileManager#getRunDirectory()}, but returns it as a {@link File} instead of {@link Path}
    public static File getRunDirectoryFile() {
        return getRunDirectory().toFile();
    }
}