package es.sim.io;

import es.sim.*;

import java.io.*;
import java.nio.file.*;

import static es.sim.Main.LOGGER;

/// This class has some IO helper methods
@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {
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

    public static void clearLogs() {
        File logsDirectory = getRunDirectory().resolve("logs").toFile();
        for(File log : logsDirectory.listFiles()) {
            if(!log.getName().equals("latest.log")) {
                log.delete();
            }
        }
    }

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

    public static File getRunDirectoryFile() {
        return getRunDirectory().toFile();
    }
}