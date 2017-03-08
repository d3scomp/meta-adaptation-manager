package cz.cuni.mff.d3s.metaadaptation;

import java.io.File;
import java.io.InputStream;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;


/**
 * Simple wrapper of java.util.logging.Logger with lazy-initialized singleton
 * object and thread-safe methods
 * 
 * @author Ilias Gerostathopoulos
 * 
 */
public class Log {
	
	private static final Log INSTANCE = new Log();
	
	public static Log getInstance() {		
		return INSTANCE;
	}
	
	
	/**
	 * The default directory where the log files are placed.
	 */
	private static final String LOG_DIRECTORY = "logs";
		
	private java.util.logging.Logger logger;
	
	
	private Log() {

		// Check whether the directory for log files exists and create it if needed
		File logDirectory = new File(LOG_DIRECTORY);
		if(!logDirectory.exists() || !logDirectory.isDirectory()){
			logDirectory.mkdirs();
		}
		
		
		logger = java.util.logging.Logger.getLogger(getClass().getPackage().getName());
		String confPath = System.getProperty("java.util.logging.config.file");
		if (confPath == null || confPath.equals(""))
			confPath = "logging.properties";
		InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(confPath);
		try {
			LogManager.getLogManager().readConfiguration(inputStream);
		} catch (Exception e) {
			logger = java.util.logging.Logger.getLogger("default");
			ConsoleHandler ch = new ConsoleHandler();
			logger.addHandler(ch);			
			logger.severe("Could not load logging.properties file - falling backing to console logging");
		}
	}

	public synchronized void debug(String s) {
		logger.log(Level.FINEST, s);
	}

	public synchronized void debug(String s, Throwable t) {
		logger.log(Level.FINEST, s, t);
	}

	public synchronized void info(String s) {
		logger.log(Level.FINEST, s);
	}

	public synchronized void info(String s, Throwable t) {
		logger.log(Level.INFO, s, t);
	}

	public synchronized void warning(String s) {
		logger.log(Level.WARNING, s);
	}

	public synchronized void warning(String s, Throwable t) {
		logger.log(Level.WARNING, s, t);
	}

	public synchronized void error(String s) {
		logger.log(Level.SEVERE, s);
	}

	public synchronized void error(String s, Throwable t) {
		logger.log(Level.SEVERE, s, t);
	}

}
