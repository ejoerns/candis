package candis.client;

import android.content.Context;
import android.util.Log;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import dalvik.system.DexFile;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controls the execution of tasks.
 *
 * @author Enrico Joerns
 */
public class JobCenter {

	private final Context mContext;
	private final ClassLoader mClassLoader;
	private static final String TAG = JobCenter.class.getName();
	private final Map<String, List<Class>> mTaskClassesMap = new HashMap<String, List<Class>>();
	private final Map<String, Class> mTaskMap = new HashMap<String, Class>();
	private final List<JobCenterHandler> mHandlerList = new LinkedList<JobCenterHandler>();
	private Map<String, DistributedJobParameter> mInitialParameterMap =
					new HashMap<String, DistributedJobParameter>();

	public JobCenter(final Context context, final ClassLoader cl) {
		mContext = context;
		mClassLoader = cl;
	}

	/**
	 *
	 * @param binary
	 */
	public void loadBinary(final String runnableID, final byte[] binary) {
		String filename = runnableID.concat(".jar");
		Log.v(TAG, String.format("Saving jar to file %s", filename));
		final File dexInternalStoragePath = new File(mContext.getFilesDir(), filename);

		writeByteArrayToFile(binary, dexInternalStoragePath);

		loadClassesFromJar(runnableID, dexInternalStoragePath);

	}

	public DistributedJobResult executeTask(final String runnableID, final DistributedJobParameter param) {
		DistributedJobResult result = null;

		if (!mTaskMap.containsKey(runnableID)) {
			Log.e(TAG, String.format("Task with UUID %s not found", runnableID));
			return null;
		}

		// notify handlers about start
		for (JobCenterHandler handler : mHandlerList) {
			handler.onJobExecutionStart(runnableID);
		}

		// try to instanciate class
		try {
			DistributedRunnable currentTask = (DistributedRunnable) mTaskMap.get(runnableID).newInstance();
			currentTask.setInitialParameter(mInitialParameterMap.get(runnableID));
			result = currentTask.runJob(param);
		}
		catch (InstantiationException ex) {
			Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (IllegalAccessException ex) {
			Logger.getLogger(JobCenter.class.getName()).log(Level.SEVERE, null, ex);
		}

		// notify handlers about end
		for (JobCenterHandler handler : mHandlerList) {
			handler.onJobExecutionDone(runnableID);
		}

		return result;
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
	 * @param runnableID
	 * @param jarfile
	 */
	private void loadClassesFromJar(final String runnableID, final File jarfile) {

		if (mTaskClassesMap.containsKey(runnableID)) {
			Log.w(TAG, String.format("Warning: Task with UUID %s already loaded", runnableID));
		}
		else {
			mTaskClassesMap.put(runnableID, new LinkedList<Class>());
		}


		// load all available classes
		String path = jarfile.getPath();
		try {
			// load dexfile
			DexFile dx = DexFile.loadDex(
							path,
							File.createTempFile("opt", "dex", mContext.getCacheDir()).getPath(),
							0);

			// extract all available classes
			for (Enumeration<String> classNames = dx.entries(); classNames.hasMoreElements();) {
				String className = classNames.nextElement();
				Log.i(TAG, String.format("found class: %s", className));
				try {
					// TODO: do only forName() here?
					final Class<Object> loadedClass = (Class<Object>) mClassLoader.loadClass(className);
					Log.i(TAG, String.format("Loaded class: %s", className));
					// add associated classes to task class list
					mTaskClassesMap.get(runnableID).add(loadedClass);
					// add task class to task list
					if (DistributedRunnable.class.isAssignableFrom(loadedClass)) {
						mTaskMap.put(runnableID, loadedClass);
					}
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
	public boolean loadInitialParameter(final String runnableID, final Object o) {
		if (!(o instanceof DistributedJobParameter)) {
			Log.e(TAG, "Initial Parameter not of class 'DistributedParameter'");
			Log.e(TAG, String.format("Is of class %s", o.getClass().getName()));
			return false;
		}
		mInitialParameterMap.put(runnableID, (DistributedJobParameter) o);
		Log.i(TAG, "Initial Parameter loaded");

		for (JobCenterHandler handler : mHandlerList) {
			handler.onInitialParameterReceived(runnableID);
		}

		return true;
	}

	/**
	 *
	 * @todo: needed?
	 * @param o
	 */
	public boolean loadJob(final String runnableID, final Object o) {
		if (!(o instanceof DistributedJobParameter)) {
			Log.e(TAG, "Initial Parameter not of class 'DistributedParameter'");
			return false;
		}
		Log.i(TAG, "Initial Parameter dummy-loaded");
		return true;
	}

	/**
	 *
	 * @param handler
	 */
	public void addHandler(final JobCenterHandler handler) {
		mHandlerList.add(handler);
	}
}
