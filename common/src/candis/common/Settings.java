package candis.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Settings management.
 *
 * Recommended usage:
 *
 * Provide (read only) default properties file "defaultproperties" and do:
 * @code
 * try {
 * Settings.load(new File("settings.properties"));
 * } catch (FileNotFoundException ex) {
 * Settings.init(MainActivity.class.getResourceAsStream("defaultsettings.properties"));
 * }
 *
 * @endcode
 *
 * @author Enrico Joerns
 */
public class Settings {

	private static final boolean DEBUG = true;
	private static final String TAG = "Settings";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	// Holds all key-value pairs
	public static Properties mKeyMap = null;

	private Settings() {
	}

	/**
	 * Loads settings from a Ressource file
	 *
	 * @param c Context
	 * @param id Inputstream to load from
	 */
	public static void load(final File file) throws FileNotFoundException {
		load(new FileInputStream(file));
	}

	/**
	 * Intended to be called with stream returned getRessourceAsStream().
	 *
	 * Sets default values.
	 *
	 * @param is (ressource) InputStream to read from
	 */
	public static void load(final InputStream is) {
		mKeyMap = new Properties();
		try {
			mKeyMap.load(is);
			if (DEBUG) {
				for (Map.Entry<Object, Object> e : mKeyMap.entrySet()) {
					LOGGER.log(Level.INFO, String.format(
									"Loaded Property: %s=%s", e.getKey(), e.getValue()));
				}
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Stores properties to given file.
	 *
	 * @param file
	 */
	public static void store(final File file) {
		try {
			OutputStream out = new FileOutputStream(file);
			mKeyMap.store(out, "This is an optional header comment string");
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	private static void checkKey(final String key) {
		if (mKeyMap == null) {
			LOGGER.log(Level.WARNING, "Properties file not found, loading default");
		}
		if (key == null) {
			throw new NullPointerException("Key is null");
		}
	}

	public static int getInt(final String key) {
		checkKey(key);
		return Integer.valueOf(mKeyMap.getProperty(key));
	}

	public static String getString(final String key) {
		checkKey(key);
		return mKeyMap.getProperty(key);
	}

	public static boolean getBoolean(final String key) {
		checkKey(key);
		return Boolean.parseBoolean(mKeyMap.getProperty(key));
	}

	public static void setInt(final String key, final int value) {
		checkKey(key);
		mKeyMap.setProperty(key, Integer.toString(value));
	}

	public static void setString(final String key, final String value) {
		checkKey(key);
		mKeyMap.setProperty(key, value);
	}

	public static void setBoolean(final String key, final boolean value) {
		checkKey(key);
		mKeyMap.setProperty(key, Boolean.toString(value));
	}
}
