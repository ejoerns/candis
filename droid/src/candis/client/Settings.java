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

	public static void load(Context c) {
		Settings.con = c;
		InputStream ins = con.getResources().openRawResource(R.raw.settings);
		ResourceBundle bundle = null;
		try {
			bundle = new PropertyResourceBundle(ins);
		} catch (IOException ex) {
			Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
		}
		Set<String> keys = bundle.keySet();

		for (String key : keys) {
			mKeyMap.put(key, bundle.getString(key));
			Log.v(TAG, key + ":" + mKeyMap.get(key));
		}
	}

	/*
	 void test() {
	 Writer writer = null;
	 Reader reader = null;
	 try {
	 FileOutputStream fos = con.openFileOutput(path, Context.MODE_PRIVATE);
	 writer = new OutputStreamWriter(fos);
	 //			fos.close();


	 //			path = con.getFilesDir() + "/" + path;
	 Log.v(TAG, "File: " + writer.toString());

	 //			writer = new FileWriter(path);

	 Properties prop1 = new Properties(System.getProperties());
	 prop1.setProperty("MeinNameIst", "Forrest Gump");
	 prop1.store(writer, "Eine Insel mit zwei Bergen");

	 //			reader = new FileReader(fos.getFD());

	 Properties prop2 = new Properties();
	 //			prop2.load(reader);
	 prop2.list(System.out);
	 } catch (IOException e) {
	 e.printStackTrace();
	 } finally {
	 try {
	 writer.close();
	 } catch (Exception e) {
	 }
	 try {
	 reader.close();
	 } catch (Exception e) {
	 }
	 }
	 }*/
	public static int getInt(final String key) {
		return Integer.valueOf(mKeyMap.get(key));
	}

	public static String getString(final String key) {
		return mKeyMap.get(key);
	}
}
