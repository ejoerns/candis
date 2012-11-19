package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class Settings {

	private static final String TAG = "Settings";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	// Holds all key-value pairs
	public static Map<String, String> mKeyMap = null;

	private Settings() {
	}

	/**
	 * Loads settings from a Ressource file
	 *
	 * @param c Context
	 * @param id Inputstream to load from
	 */
	public static void load(final InputStream ins) {
		mKeyMap = new HashMap<String, String>();
		ResourceBundle bundle = null;
		try {
			bundle = new PropertyResourceBundle(ins);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		Set<String> keys = bundle.keySet();

		for (String key : keys) {
			mKeyMap.put(key, bundle.getString(key));
			LOGGER.log(Level.INFO, String.format("Loaded: %s=%s", key, mKeyMap.get(key)));
		}
	}

	public static int getInt(final String key) {
		if (mKeyMap == null) {
			throw new NullPointerException(
							"Empty properties set. Call load() to load properties from file");
		}
		if (key == null) {
			throw new NullPointerException("Property not found");
		}
		return Integer.valueOf(mKeyMap.get(key));
	}

	public static String getString(final String key) {
		if (mKeyMap == null) {
			throw new NullPointerException(
							"Empty properties set. Call load() to load properties from file");
		}
		if (key == null) {
			throw new NullPointerException("Property not found");
		}
		return mKeyMap.get(key);
	}
}
