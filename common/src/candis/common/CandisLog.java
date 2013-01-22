package candis.common;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class CandisLog {

	public enum CandisLogLevel {

		VERBOSE(0, "VERBOSE", Level.INFO),
		DEBUG(10, "DEBUG", Level.INFO),
		INFO(20, "INFO", Level.INFO),
		WARNING(30, "WARNING", Level.WARNING),
		ERROR(40, "ERROR", Level.SEVERE),
		OFF(1000, "OFF", Level.OFF);

		CandisLogLevel(int iLevel, String sLevel, Level lLevel) {
			this.iLevel = iLevel;
			this.sLevel = sLevel;
			this.lLevel = lLevel;
		}
		public final int iLevel;
		public final String sLevel;
		public final Level lLevel;
	};
	private static Map<String, CandisLogLevel> levelMap = generateLevelMap();

	private static Map<String, CandisLogLevel> generateLevelMap() {
		Map<String, CandisLogLevel> result = new HashMap<String, CandisLogLevel>();
		result.put("ALL", CandisLogLevel.VERBOSE);
		result.put("CONFIG", CandisLogLevel.DEBUG);
		result.put("FINE", CandisLogLevel.DEBUG);
		result.put("DEBUG", CandisLogLevel.DEBUG);
		result.put("FINEST", CandisLogLevel.VERBOSE);
		result.put("FINER", CandisLogLevel.VERBOSE);
		result.put("VERBOSE", CandisLogLevel.VERBOSE);
		result.put("INFO", CandisLogLevel.INFO);
		result.put("OFF", CandisLogLevel.OFF);
		result.put("SEVERE", CandisLogLevel.ERROR);
		result.put("ERROR", CandisLogLevel.ERROR);
		result.put("WARNING", CandisLogLevel.WARNING);




		return result;
	}
	private static CandisLogLevel mLevel = CandisLogLevel.INFO;

	public static void level(CandisLogLevel level) {
		mLevel = level;
	}

	public static void e(String tag, String msg) {
		logp(tag, CandisLogLevel.ERROR, msg);
	}

	public static void e(String msg) {
		logp(null, CandisLogLevel.ERROR, msg);
	}

	public static void w(String tag, String msg) {
		logp(tag, CandisLogLevel.WARNING, msg);
	}

	public static void w(String msg) {
		logp(null, CandisLogLevel.WARNING, msg);
	}

	public static void i(String tag, String msg) {
		logp(tag, CandisLogLevel.INFO, msg);
	}

	public static void i(String msg) {
		logp(null, CandisLogLevel.INFO, msg);
	}

	public static void d(String tag, String msg) {
		logp(tag, CandisLogLevel.DEBUG, msg);
	}

	public static void d(String msg) {
		logp(null, CandisLogLevel.DEBUG, msg);
	}

	public static void v(String tag, String msg) {
		logp(tag, CandisLogLevel.VERBOSE, msg);
	}

	public static void v(String msg) {
		logp(null, CandisLogLevel.VERBOSE, msg);
	}

	public static void log(String tag, CandisLogLevel level, String msg) {
		logp(tag, level, msg);
	}

	private static void log(CandisLogLevel level, String msg) {
		logp(null, level, msg);
	}

	private static void logp(String tag, CandisLogLevel level, String msg) {
		StackTraceElement stack = (Thread.currentThread().getStackTrace())[3];
		if (tag == null) {
			tag = stack.getClassName();
		}
		String defaultLevel = LogManager.getLogManager().getProperty(tag);
		CandisLogLevel cLevel = mLevel;
		if (defaultLevel != null && levelMap.containsKey(defaultLevel)) {
			cLevel = levelMap.get(defaultLevel);
		}

		if (cLevel.iLevel <= level.iLevel) {

			Logger.getLogger(tag).logp(level.lLevel, stack.getClassName(), stack.getMethodName(), String.format("%s: %s", level.sLevel, msg));
		}

	}
}
