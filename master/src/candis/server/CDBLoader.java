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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
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
	private DistributedControl mDistributedControl;
	private Class mDistributedRunnable;
	private CDBContext mCDBContext;
	private final ClassLoaderWrapper mClassLoaderWrapper;
	private static int mCDBid = 0;

	/**
	 * Creates new CDB loader.
	 *
	 * @param cdbfile CDB file to load
	 * @param cloader Parent ClassLoader
	 */
	public CDBLoader() {
		mClassLoaderWrapper = new ClassLoaderWrapper();
	}

	/**
	 * Returns extracted DistributedControl.
	 *
	 * @return extracted DistributedControl
	 */
	public final DistributedControl getDistributedControl() {
		return mDistributedControl;
	}

	public final DistributedRunnable getDistributedRunnable() {
		try {
			return (DistributedRunnable) mDistributedRunnable.newInstance();
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
	 * Returns loaded droid binary file.
	 *
	 * @return loaded droid binary
	 */
	public final File getDroidBinary() {
		return mCDBContext.getDroidBin();
	}

	/**
	 * Returns the ClassLoader the CDB classes were loaded with. Use this
	 * ClassLoader to process handling of cdb classes!
	 *
	 * @return ClassLoader the CDB classes were loaded with
	 */
	public ClassLoaderWrapper getClassLoaderWrapper() {
		return mClassLoaderWrapper;
	}

	/**
	 * Loads classes from cdb file.
	 *
	 * @param cdbfile
	 * @return ID for the CDB file.
	 */
	public int loadCDB(final File cdbfile) throws Exception {
		final String projectPath = cdbfile.getName().substring(0, cdbfile.getName().lastIndexOf('.'));
		List<String> classList;

		mCDBContext = new CDBContext(projectPath);
		extractCandisDistributedBundle(cdbfile, mCDBContext);

		try {
			List<URL> urls = new LinkedList<URL>();
			for(int i=0; i < mCDBContext.getLibCount(); i++) {
				urls.add(mCDBContext.getLib(i).toURI().toURL());
			}
			urls.add(mCDBContext.getServerBin().toURI().toURL());
			mClassLoaderWrapper.set(
							new URLClassLoader(
							urls.toArray(new URL[]{}),
							this.getClass().getClassLoader()));


			for(int i=0; i < mCDBContext.getLibCount(); i++) {
				classList = getClassNamesInJar(mCDBContext.getLib(i).getPath());

				for (String classname : classList) {
					// finds the DistributedControl instance
					Class classToLoad = mClassLoaderWrapper.get().loadClass(classname);
					if ((!DistributedJobParameter.class.isAssignableFrom(classToLoad))
									&& (!DistributedJobResult.class.isAssignableFrom(classToLoad))
									&& (!DistributedRunnable.class.isAssignableFrom(classToLoad))) {
						LOGGER.log(Level.INFO, "Loaded class with non-default interface: {0}", classToLoad.getName());
					}
					else if (DistributedRunnable.class.isAssignableFrom(classToLoad))
					{
						mDistributedRunnable = classToLoad;
					}
					else {
						LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
					}
				}
			}

			// load server binary
			classList = getClassNamesInJar(mCDBContext.getServerBin().getPath());

			for (String classname : classList) {
				System.out.println("Trying to load class: " + classname);
				// finds the DistributedControl instance
				Class classToLoad = mClassLoaderWrapper.get().loadClass(classname);
				if (DistributedControl.class.isAssignableFrom(classToLoad)) {
					LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
					try {
						mDistributedControl = (DistributedControl) classToLoad.newInstance();
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

		return mCDBid++;
	}

	/**
	 * Extracts candis distributed bundle to the directory given with cdbContext.
	 *
	 * File format: zip containing 3 files: - config.properties - droid binary -
	 * server binary
	 *
	 * @param cdbfile Name of cdb-file
	 * @param cdbContext
	 */
	private void extractCandisDistributedBundle(final File cdbfile, final CDBContext cdbContext) throws Exception {
		ZipFile zipFile;
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
			Properties p = new Properties();
			p.load(zipFile.getInputStream(entry));
			server_binary = p.getProperty("server.binary");
			droid_binary = p.getProperty("droid.binary");
			if (server_binary == null) {
				throw new Exception("No server binary given");
			}
			if (droid_binary == null) {
				throw new Exception("No droid binary given");
			}

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
			while ((lib = p.getProperty(String.format("server.lib.%d", libNumber))) != null) {
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
	 * Holds filenames.
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
