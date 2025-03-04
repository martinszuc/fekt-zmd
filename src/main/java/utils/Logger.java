package utils;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * Utility class for logging application events and status
 */
public class Logger {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("JPEG_App");
    private static boolean isInitialized = false;

    /**
     * Initialize the logger with file and console handlers
     */
    public static void init() {
        if (isInitialized) return;

        try {
            // Create logs directory if it doesn't exist
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdir();
            }

            // Configure logger with a file handler and console handler
            FileHandler fileHandler = new FileHandler("logs/jpeg_app.log", true);
            fileHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tF %1$tT] [%2$s] %3$s%n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });

            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter() {
                @Override
                public String format(LogRecord record) {
                    return String.format("[%1$tT] [%2$s] %3$s%n",
                            record.getMillis(),
                            record.getLevel().getName(),
                            record.getMessage());
                }
            });

            logger.addHandler(fileHandler);
            logger.addHandler(consoleHandler);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.INFO);

            isInitialized = true;
            info("Logger initialized");
        } catch (IOException e) {
            System.err.println("Failed to initialize logger: " + e.getMessage());
        }
    }

    /**
     * Log an informational message
     * @param message The message to log
     */
    public static void info(String message) {
        if (!isInitialized) init();
        logger.info(message);
    }

    /**
     * Log a warning message
     * @param message The message to log
     */
    public static void warning(String message) {
        if (!isInitialized) init();
        logger.warning(message);
    }

    /**
     * Log an error message
     * @param message The message to log
     */
    public static void error(String message) {
        if (!isInitialized) init();
        logger.severe(message);
    }

    /**
     * Log a debug message
     * @param message The message to log
     */
    public static void debug(String message) {
        if (!isInitialized) init();
        logger.fine(message);
    }
}