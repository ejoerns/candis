package candis.client;

import android.content.Context;
import android.util.Log;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedTask;
import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls the execution of tasks.
 *
 * @author Enrico Joerns
 */
public class JobCenter {

	private final Context mContext;
	private final ClassLoader mClassLoader;
	private static final String TAG = JobCenter.class.getName();
	private Map<String, DistributedTask> mTaskMap = new HashMap<String, DistributedTask>();

	public JobCenter(final Context context, final ClassLoader cl) {
		mContext = context;
		mClassLoader = cl;
	}

	/**
	 *
	 * @param binary
	 */
	public void loadBinary(byte[] binary) {
		final File dexInternalStoragePath = new File(mContext.getFilesDir(), "foo.jar");

		writeByteArrayToFile(binary, dexInternalStoragePath);

		loadClassesFromJar(dexInternalStoragePath);

	}

	/**
	 *
	 * @param data
	 * @param filename
	 */
	private void writeByteArrayToFile(final byte[] data, final File filename) {
		BufferedOutputStream bos = null;

		try {
			//create an object of FileOutputStream
			FileOutputStream fos = new FileOutputStream(filename);
			//create an object of BufferedOutputStream
			bos = new BufferedOutputStream(fos);
			bos.write((byte[]) data);
			System.out.println("File written");
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("Specified file not found" + fnfe);
		}
		catch (IOException ioe) {
			System.out.println("Error while writing file" + ioe);
		}
		finally {
			if (bos != null) {
				try {
					bos.flush();
					bos.close();
				}
				catch (Exception e) {
				}
			}
		}
	}

	/**
	 * As it name claims, it loads classes from the given jar.
	 *
	 * @param jarfile
	 */
	private void loadClassesFromJar(final File jarfile) {
		// load all available classes
		String path = jarfile.getPath();
		try {
			DexFile dx = DexFile.loadDex(
							path,
							File.createTempFile("opt", "dex", mContext.getCacheDir()).getPath(),
							0);
			for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
				String className = classNames.nextElement();
				Log.i(TAG, String.format("found class: %s", className));
				Class<?> aClass = null;
				try {
					final File tmpDir = mContext.getDir("dex", 0);

					final Class<Object> classToLoad = (Class<Object>) mClassLoader.loadClass(className);
					Log.i(TAG, String.format("Loaded class: %s", className));
				}
				catch (Exception ex) {
					Log.i(TAG, ex.toString());
				}
			}
		}
		catch (IOException e) {
			System.out.println("Error opening " + path);
		}
	}

	/**
	 *
	 * @param o
	 */
	void loadInitialParameter(Object o) {
		if (!(o instanceof DistributedParameter)) {
			Log.e(TAG, "Initial Parameter not of class 'DistributedParameter'");
		}
		else {
			Log.i(TAG, "Initial Parameter dummy-loaded");
		}
	}

	/**
	 *
	 * @param o
	 */
	void loadParameter(Object o) {
		if (!(o instanceof DistributedParameter)) {
			Log.e(TAG, "Initial Parameter not of class 'DistributedParameter'");
		}
		else {
			Log.i(TAG, "Initial Parameter dummy-loaded");
		}
	}
}
