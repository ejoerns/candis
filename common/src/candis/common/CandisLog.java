package candis.common;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class CandisLog {

  public static final int VERBOSE = 0;
  public static final int DEBUG = 10;
  public static final int INFO = 20;
  public static final int WARNING = 30;
  public static final int ERROR = 40;
  private static int mLevel = WARNING;

  public static void level(int level) {
    mLevel = level;
  }

  public static void e(String tag, String msg) {
    if (mLevel <= ERROR) {
      log(tag, "ERROR", msg);
    }
  }

  public static void w(String tag, String msg) {
    if (mLevel <= WARNING) {
      log(tag, "WARNING", msg);
    }
  }

  public static void i(String tag, String msg) {
    if (mLevel <= INFO) {
      log(tag, "INFO", msg);
    }
  }

  public static void d(String tag, String msg) {
    if (mLevel <= DEBUG) {
      log(tag, "DEBUG", msg);
    }
  }

  public static void v(String tag, String msg) {
    if (mLevel <= VERBOSE) {
      log(tag, "VERBOSE", msg);
    }
  }

  public static void log(String tag, String level, String msg) {
    Logger.getLogger(tag).log(Level.INFO, String.format("%s: %s", level, msg));
  }
}
