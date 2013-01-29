package candis.server;

import candis.common.ClassLoaderWrapper;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Loads a CDB file.
 *
 * A CDB file is a project file for candis containing a droid binary, a server
 * binary and additional libraries.
 *
 * File format: Zip-Compressed file with .cdb extendsion. Filenames are
 * described in a 'config.properties' file.
 *
 * @author Enrico Joerns
 */
public class CDBLoader {

	private static final Logger LOGGER = Logger.getLogger(CDBLoader.class.getName());
	private final Map<String, DistributedControl> mDistributedControl = new HashMap<String, DistributedControl>();
	private final Map<String, Class> mDistributedRunnable = new HashMap<String, Class>();
	private final Map<String, String> mTaskNames = new HashMap<String, String>();
	private final Map<String, CDBContext> mCDBContextMap = new HashMap<String, CDBContext>();
	private final Set<String> mKnownTaskIDs = new HashSet<String>();
	private final Map<String, ClassLoader> mClassLoaderMap = new HashMap<String, ClassLoader>();
//	private final ClassLoaderWrapper mClassLoaderWrapper;
	private static int mCDBid = 42;

	/**
	 * Creates new CDB loader.
	 *
	 * @param cdbfile CDB file to load
	 * @param cloader Parent ClassLoader
	 */
	public CDBLoader() {
//		mClassLoaderWrapper = new ClassLoaderWrapper();
	}

	/**
	 * Returns extracted DistributedControl.
	 *
	 * @return extracted DistributedControl
	 */
	public final DistributedControl getDistributedControl(String id) {
		return mDistributedControl.get(id);
	}

	public final DistributedRunnable getDistributedRunnable(String id) {
		try {
			return (DistributedRunnable) mDistributedRunnable.get(id).newInstance();
		}
		catch (InstantiationException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IllegalAccessException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return null;

	}

	/**
	 *
	 * @param runnableID
	 * @return
	 */
	public ClassLoader getClassLoader(String runnableID) {
		return mClassLoaderMap.get(runnableID);
	}

	/**
	 *
	 * @param id
	 * @return
	 */
	public String getTaskName(String id) {
		return mTaskNames.get(id);
	}

	/**
	 * Returns loaded droid binary file.
	 *
	 * @return loaded droid binary
	 */
	public final File getDroidBinary(String id) {
		if (!mKnownTaskIDs.contains(id)) {
			LOGGER.log(Level.SEVERE, "Binary for id {0} not found", id);
			return null;
		}
		return mCDBContextMap.get(id).getDroidBin();
	}

	/**
	 * Returns the ClassLoader the CDB classes were loaded with. Use this
	 * ClassLoader to process handling of cdb classes!
	 *
	 * @return ClassLoader the CDB classes were loaded with
	 */
//	public ClassLoaderWrapper getClassLoaderWrapper() {
//		return mClassLoaderWrapper;
//	}
	/**
	 * Loads classes from cdb file.
	 *
	 * Returns ID that can be used to acces CDB data later on.
	 *
	 * @param cdbfile
	 * @return ID for the CDB file.
	 */
	public String loadCDB(final File cdbfile) throws Exception {
		final String projectPath = cdbfile.getName().substring(0, cdbfile.getName().lastIndexOf('.'));
		List<String> classList;
		String newID = String.format("%05d", mCDBid++);
		LOGGER.log(Level.INFO, "Loading CDB file with ID {0}", newID);

		final CDBContext newCDBContext = new CDBContext(projectPath);
		extractCandisDistributedBundle(cdbfile, newID, newCDBContext);

		try {
			List<URL> urls = new LinkedList<URL>();
			for (int i = 0; i < newCDBContext.getLibCount(); i++) {
				urls.add(newCDBContext.getLib(i).toURI().toURL());
			}
			urls.add(newCDBContext.getServerBin().toURI().toURL());
			mClassLoaderMap.put(newID, new URLClassLoader(
							urls.toArray(new URL[]{}),
							this.getClass().getClassLoader()));


			for (int i = 0; i < newCDBContext.getLibCount(); i++) {
				classList = getClassNamesInJar(newCDBContext.getLib(i).getPath());

				for (String classname : classList) {
					// finds the DistributedControl instance
					Class classToLoad = mClassLoaderMap.get(newID).loadClass(classname);
					if ((!DistributedJobParameter.class.isAssignableFrom(classToLoad))
									&& (!DistributedJobResult.class.isAssignableFrom(classToLoad))
									&& (!DistributedRunnable.class.isAssignableFrom(classToLoad))) {
						LOGGER.log(Level.INFO, "Loaded class with non-default interface: {0}", classToLoad.getName());
					}
					else if (DistributedRunnable.class.isAssignableFrom(classToLoad)) {
						mDistributedRunnable.put(newID, classToLoad);
					}
					else {
						LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
					}
				}
			}

			// load server binary
			classList = getClassNamesInJar(newCDBContext.getServerBin().getPath());

			for (String classname : classList) {
				System.out.println("Trying to load class: " + classname);
				// finds the DistributedControl instance
				Class classToLoad = mClassLoaderMap.get(newID).loadClass(classname);
				if (DistributedControl.class.isAssignableFrom(classToLoad)) {
					LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
					try {
						mDistributedControl.put(newID, (DistributedControl) classToLoad.newInstance());
					}
					catch (InstantiationException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
					catch (IllegalAccessException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
				}
			}

		}
		catch (MalformedURLException ex) {
			Logger.getLogger(CDBLoader.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (SecurityException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		mCDBContextMap.put(newID, newCDBContext);
		mKnownTaskIDs.add(newID);
		// convert to string ID
		return newID;
	}

	/**
	 * Extracts candis distributed bundle to the directory given with cdbContext.
	 *
	 * File format: zip containing 3 files: - config.properties - droid binary -
	 * server binary
	 *
	 * @param cdbfile Name of cdb-file
	 * @param cdbContext CDB context
	 */
	private void extractCandisDistributedBundle(
					final File cdbfile,
					final String taskID,
					final CDBContext cdbContext)
					throws Exception {
		ZipFile zipFile;
		String name;
		String server_binary;
		String droid_binary;
		int libNumber = 0;

		try {
			zipFile = new ZipFile(cdbfile);

			// create new directory to store unzipped project files
			File newDir = cdbContext.getProjectDir();
			if (!newDir.exists()) {
				if (!newDir.mkdir()) {
					LOGGER.log(Level.WARNING, "Project directory {0} could not be created", newDir);
				}
			}

			// try to load filenames from properties file
			ZipEntry entry = zipFile.getEntry("config.properties");
			if (entry == null) {
				throw new FileNotFoundException("cdb does not have a 'config.properties'");
			}
			Properties props = new Properties();
			props.load(zipFile.getInputStream(entry));
			name = props.getProperty("name");
			server_binary = props.getProperty("server.binary");
			droid_binary = props.getProperty("droid.binary");
			if (name == null) {
				throw new Exception("No task name given");
			}
			if (server_binary == null) {
				throw new Exception("No server binary given");
			}
			if (droid_binary == null) {
				throw new Exception("No droid binary given");
			}

			// Set task name
			mTaskNames.put(taskID, name);

			// load server binary
			entry = zipFile.getEntry(server_binary);
			copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
							new FileOutputStream(cdbContext.getServerBin())));
			LOGGER.log(Level.FINE, "Extracted server binary: {0}", entry.getName());

			// load droid binary
			entry = zipFile.getEntry(droid_binary);
			copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
							new FileOutputStream(cdbContext.getDroidBin())));
			LOGGER.log(Level.FINE, "Extracted droid binary: {0}", entry.getName());

			// load libs
			String lib;
			while ((lib = props.getProperty(String.format("server.lib.%d", libNumber))) != null) {
				File libName = cdbContext.getLibByNumber(libNumber);
				cdbContext.addLib(libName);
				entry = zipFile.getEntry(lib);
				copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
								new FileOutputStream(libName)));
				LOGGER.log(Level.FINE, "Extracted lib: {0}", entry.getName());
				libNumber++;
			}
			LOGGER.log(Level.FINE, "{0} libs found", libNumber);
		}
		catch (ZipException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	private static final void copyInputStream(InputStream in, OutputStream out)
					throws IOException {
		byte[] buffer = new byte[1024];
		int len;

		while ((len = in.read(buffer)) >= 0) {
			out.write(buffer, 0, len);
		}

		in.close();
		out.close();
	}

	/**
	 * Finds all class names in a jar file.
	 *
	 * @param jarName Jar file to search in
	 * @return List of all full class names
	 */
	public static List<String> getClassNamesInJar(final String jarName) {
		List<String> classes = new ArrayList<String>();
		JarInputStream jarFile = null;
		try {
			jarFile = new JarInputStream(new FileInputStream(jarName));
			JarEntry jarEntry;

			while (true) {
				jarEntry = jarFile.getNextJarEntry();
				if (jarEntry == null) {
					break;
				}
				if (jarEntry.getName().endsWith(".class")) {
					LOGGER.log(Level.FINE, "Found class: " + jarEntry.getName().replaceAll("/", "\\."));
					classes.add(removeFileExtension(jarEntry.getName().replaceAll("/", "\\.")));
				}
			}
		}
		catch (Exception e) {

			LOGGER.log(Level.WARNING, "hier ist was doof", e);
		}
		finally {
			try {
				if (jarFile != null) {
					jarFile.close();
				}
			}
			catch (IOException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
		}
		return classes;
	}

	/**
	 * Removes the filename extension from filename.
	 *
	 * @param s filename to process
	 * @return filename without extension
	 */
	public static String removeFileExtension(String s) {

		String separator = System.getProperty("file.separator");
		String filename;

		// Remove the path upto the filename.
		int lastSeparatorIndex = s.lastIndexOf(separator);
		if (lastSeparatorIndex == -1) {
			filename = s;
		}
		else {
			filename = s.substring(lastSeparatorIndex + 1);
		}

		// Remove the extension.
		int extensionIndex = filename.lastIndexOf(".");
		if (extensionIndex == -1) {
			return filename;
		}

		return filename.substring(0, extensionIndex);
	}

	/**
	 * Holds filenames. Used internal only.
	 */
	private class CDBContext {

		private String mPath;
		private File mDroidBin;
		private File mServerBin;
		private List<File> mLibs;

		/**
		 * Creates CDB context for the given path.
		 *
		 * @param path project path
		 */
		public CDBContext(final String path) {
			mPath = path;
			mDroidBin = new File(mPath, "droid.binary.dex");
			mServerBin = new File(mPath, "server.binary.jar");
			mLibs = new LinkedList<File>();
		}

		public int getLibCount() {
			return mLibs.size();
		}

		public void addLib(File lib) {
			mLibs.add(lib);
		}

		public File getProjectDir() {
			return new File(mPath);
		}

		public File getDroidBin() {
			return mDroidBin;
		}

		public File getServerBin() {
			return mServerBin;
		}

		public File getLibByNumber(final int n) {
			return new File(mPath, String.format("server.lib.%d.jar", n));
		}

		public File getLib(final int n) {
			//return new File(mPath, String.format("server.lib.%d.jar", n));
			return mLibs.get(n);
		}
	}
}
