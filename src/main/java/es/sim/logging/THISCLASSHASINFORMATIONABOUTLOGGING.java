package es.sim.logging;

import es.sim.*;
import org.slf4j.*;

public class THISCLASSHASINFORMATIONABOUTLOGGING {
    private static void LOGINFORMATION() {
        //A brief explanation on logging and how to use Main.LOGGER:

        //Logging is useful to display handy information, like if a certain piece of code ran
        //successfully, or with an error
        //It is preferred to use over System.out.println() as it gives more information and supports string parameters
        //(more on that later)

        //Retrieve a custom logger by calling LoggerFactory.getLogger()
        Logger customLogger = LoggerFactory.getLogger("customLogger");
        Logger mainLogger = LoggerFactory.getLogger(Main.class);
        //This supports both a String or Class<?> parameter

        //Logging has 5 different levels of importance, from lowest to highest:
        //TRACE -> Displays trace information, not used often
        //DEBUG -> Displays debug information, information that is not useful to the user, but to the developer
        //INFO -> Displays generic info that is useful for both the user and developer
        //WARN -> Displays a warning message
        //ERROR -> Used when an error happens

        //To display a log message at each level, all of them have their own designated method:
        customLogger.trace("trace message");
        customLogger.debug("debug message");
        customLogger.info("info message");
        customLogger.warn("warn message");
        customLogger.error("error message");

        //You can also use variables to print them to the console
        int x = 3;
        int y = 5;
        mainLogger.debug("X value: " + x);
        mainLogger.debug("Y value: " + y);
        mainLogger.debug("X value: " + x + ", Y value: " + y);

        //Instead of using String concatenation (which looks ugly), we can use parametrized log messages
        mainLogger.debug("X value: {}", x);
        mainLogger.debug("Y value: {}", y);
        mainLogger.debug("X value: {}, Y value: {}", x, y);
        //Look how clean!


        //Congratulations!
        //You now know literarily everything about logging!
        //Here is your degree in logging:
        //https://www.youtube.com/watch?v=xvFZjo5PgG0
    }
}
