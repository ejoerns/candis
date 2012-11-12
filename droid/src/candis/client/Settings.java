/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author enrico
 */
public class Settings {

	private static final String TAG = "Settings";
	private static Context con;
	// Holds all key-value pairs
	public static Map<String, String> mKeyMap = new HashMap<String, String>();

	/**
	 * Loads settings from a Ressource file
	 *
	 * @param c Context
	 * @param id Id of file
	 */
	public static void load(final Context c, final int id) {
		Settings.con = c;
		InputStream ins = con.getResources().openRawResource(id);
		ResourceBundle bundle = null;
		try {
			bundle = new PropertyResourceBundle(ins);
		} catch (IOException ex) {
			Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
		}
		Set<String> keys = bundle.keySet();

		for (String key : keys) {
			mKeyMap.put(key, bundle.getString(key));
			Log.v(TAG, "Loaded: " + key + "=" + mKeyMap.get(key));
		}
	}

	public static int getInt(final String key) {
		if (key == null) return 0;
		return Integer.valueOf(mKeyMap.get(key));
	}

	public static String getString(final String key) {
		if (key == null) return "";
		return mKeyMap.get(key);
	}
}
