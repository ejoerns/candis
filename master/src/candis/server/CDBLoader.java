package candis.server;

import candis.common.ClassloaderObjectInputStream;
import candis.common.Utilities;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.MessageDigest;
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

	public final DistributedJobResult[] deserializeJob(String cdbID, byte[] unserialized) {
		ObjectInputStream objInstream;
		Object object = null;
		try {
			objInstream = new ClassloaderObjectInputStream(
							new ByteArrayInputStream(unserialized),
							getClassLoader(cdbID));
			object = objInstream.readObject();
			objInstream.close();
//      obj = new ClassloaderObjectInputStream(new ByteArrayInputStream(lastUnserializedJob), mClassLoaderWrapper).readObject();
		}
		catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			return null;
		}

		return (DistributedJobResult[]) object;
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
	 * @param cdbID ID of cdb
	 * @return
	 */
	public String getTaskName(String cdbID) {
		return mTaskNames.get(cdbID);
	}

	/**
	 * Returns loaded droid binary file.
	 *
	 * @return loaded droid binary
	 */
	public final byte[] getDroidBinary(String cdbID) {
		byte[] buffer = null;

		if (!mKnownTaskIDs.contains(cdbID)) {
			LOGGER.log(Level.SEVERE, "Binary for id {0} not found", cdbID);
			return null;
		}

		final File file = mCDBContextMap.get(cdbID).getDroidBin();
		try {
			final RandomAccessFile rfile = new RandomAccessFile(file, "r");
			buffer = new byte[(int) file.length()];
			rfile.read(buffer);
		}
		catch (FileNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		return buffer;
	}

	/**
	 * Loads classes from cdb file.
	 *
	 * Returns ID that can be used to acces CDB data later on.
	 *
	 * @param cdbfile
	 * @return ID for the CDB file, null if the file has already been loaded
	 */
	public final String loadCDB(final File cdbfile) throws Exception {
		final String projectPath = cdbfile.getName().substring(0, cdbfile.getName().lastIndexOf('.'));
		List<String> classList;

		// Create MD5 sum as ID for CDB
		final MessageDigest digest = MessageDigest.getInstance("MD5");
		final byte[] data = new byte[(int) cdbfile.length()];
		final FileInputStream fis = new FileInputStream(cdbfile);
		try {
			fis.read(data);
		}
		finally {
			fis.close();
		}
		// Reads it all at one go. Might be better to chunk it.
		digest.update(data);
		final String newID = toHex(digest.digest());

		// check if already loaded
		if (mKnownTaskIDs.contains(newID)) {
			LOGGER.log(Level.WARNING, "File with ID {0} already loaded.", newID);
			return null;
		}

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
				classList = Utilities.getClassNamesInJar(newCDBContext.getLib(i));

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
			classList = Utilities.getClassNamesInJar(newCDBContext.getServerBin());

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
	 * @param taskID ID of task
	 * @param cdbContext CDB context
	 * @throws Exception if something fails
	 */
	private void extractCandisDistributedBundle(
					final File cdbfile,
					final String taskID,
					final CDBContext cdbContext)
					throws Exception {
		ZipFile zipFile;
		String name;
		String serverBinary;
		String droidBinary;
		int libNumber = 0;

		try {
			zipFile = new ZipFile(cdbfile);

			// create new directory to store unzipped project files
			final File newDir = cdbContext.getProjectDir();
			if (!newDir.exists() && !newDir.mkdir()) {
				LOGGER.log(Level.WARNING, "Project directory {0} could not be created", newDir);
				throw new Exception("Project directory could not be created");
			}

			// try to load filenames from properties file
			ZipEntry entry = zipFile.getEntry("config.properties");
			if (entry == null) {
				throw new FileNotFoundException("cdb does not have a 'config.properties'");
			}
			final Properties props = new Properties();
			props.load(zipFile.getInputStream(entry));
			name = props.getProperty("name");
			serverBinary = props.getProperty("server.binary");
			droidBinary = props.getProperty("droid.binary");
			if (name == null) {
				throw new Exception("No task name given");
			}
			if (serverBinary == null) {
				throw new Exception("No server binary given");
			}
			if (droidBinary == null) {
				throw new Exception("No droid binary given");
			}

			// Set task name
			mTaskNames.put(taskID, name);

			// load server binary
			entry = zipFile.getEntry(serverBinary);
			Utilities.copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
							new FileOutputStream(cdbContext.getServerBin())));
			LOGGER.log(Level.FINE, "Extracted server binary: {0}", entry.getName());

			// load droid binary
			entry = zipFile.getEntry(droidBinary);
			Utilities.copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
							new FileOutputStream(cdbContext.getDroidBin())));
			LOGGER.log(Level.FINE, "Extracted droid binary: {0}", entry.getName());

			// load libs
			String lib;
			while ((lib = props.getProperty(String.format("server.lib.%d", libNumber))) != null) {
				File libName = cdbContext.getLibByNumber(libNumber);
				cdbContext.addLib(libName);
				entry = zipFile.getEntry(lib);
				Utilities.copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
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

	public static String toHex(byte[] a) {
		StringBuilder sbuild = new StringBuilder(a.length * 2);
		for (int i = 0; i < a.length; i++) {
			sbuild.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
			sbuild.append(Character.forDigit(a[i] & 0x0f, 16));
		}
		return sbuild.toString();
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
