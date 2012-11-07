/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client;

import android.content.Context;
import android.util.Log;
import java.io.*;
import java.util.Properties;

/**
 *
 * @author enrico
 */
public class Settings {

	private String path = "properties.txt";
	private static final String TAG = "Settings";
	Context con;

	public Settings(Context con) {
		this.con = con;
	}

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
	}
}
