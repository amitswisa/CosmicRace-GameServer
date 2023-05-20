package utils.logs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoggerManager {

    private static final Logger logger = LogManager.getLogger("Console");

    public static void info(String message) {
        logger.info(message);
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void debug(String message) {logger.debug(message);}

    public static void warning(String message) {logger.warn(message);}
}
