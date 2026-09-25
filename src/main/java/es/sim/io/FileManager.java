package es.sim.io;

import es.sim.*;
import es.sim.util.*;

import java.io.*;
import java.nio.file.*;

/// This class has some helper methods related to IO
@SuppressWarnings("ResultOfMethodCallIgnored")
public class FileManager {
    ///Generates the files needed to put the save data for the program in
    public static void generateFiles() {
        File runDir = getRunDirectoryFile();
        if (!runDir.exists()) {
            runDir.mkdir();
            runDir.toPath().resolve("logs").toFile().mkdir();
            runDir.toPath().resolve("config").toFile().mkdir();
            runDir.toPath().resolve("saves").toFile().mkdir();
        }
    }

    ///Clears all logs in the logs folder, except for {@code latest.log} when the program shuts down
    public static void clearLogs() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            File logsDirectory = getRunDirectory().resolve("logs").toFile();
            for(File log : logsDirectory.listFiles()) {
                if(!log.getName().equals("latest.log")) {
                    //FIXME: for some reason the log corresponding to the current runtime cannot get deleted
                    log.delete();
                }
            }
        }));
    }

    ///Returns the Run Directory as the program. Useful to determine where the save files should be stored
    public static Path getRunDirectory() {
        Path runDir;
        if(DebugVariables.IS_DEVELOPMENT_ENVIRONMENT) {
            runDir = Paths.get("run");
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