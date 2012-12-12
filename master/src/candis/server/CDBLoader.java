package candis.server;

import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
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
 * A CDB file is a project file for candis containing a droid binary,
 * a server binary and additional libraries.
 *
 * File format: Zip-Compressed file with .cdb extendsion.
 * Filenames are described in a 'config.properties' file.
 *
 * @author Enrico Joerns
 */
public class CDBLoader {

	private static final Logger LOGGER = Logger.getLogger(ServerCommunicationIO.class.getName());
	private DistributedControl mDistributedControl;
	private CDBContext mCDBContext;

	/**
	 * Creates new CDB loader.
	 *
	 * @param cdbfile CDB file to load
	 */
	public CDBLoader(final File cdbfile) {
		loadCandisDistributedBundle(cdbfile);
	}

	/**
	 * Returns extracted DistributedControl.
	 *
	 * @return extracted DistributedControl
	 */
	public final DistributedControl getDistributedControl() {
		return mDistributedControl;
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
	 * Loads classes from cdb file.
	 *
	 * @param cdbfile
	 */
	private void loadCandisDistributedBundle(final File cdbfile) {
		final String projectPath = cdbfile.getName().substring(0, cdbfile.getName().lastIndexOf('.'));
		List<String> classList = null;

		mCDBContext = new CDBContext(projectPath);
		extractCandisDistributedBundle(cdbfile, mCDBContext);

		URLClassLoader child;
		try {
			child = new URLClassLoader(
							new URL[]{mCDBContext.getLib(0).toURI().toURL(), mCDBContext.getServerBin().toURI().toURL()},
							this.getClass().getClassLoader());

			// load lib 0
			// TODO: allow for multiple libs
			classList = getClassNamesInJar(mCDBContext.getLib(0).getPath());

			for (String classname : classList) {
				// finds the DistributedControl instance
				Class classToLoad = child.loadClass(classname);
				if ((!DistributedParameter.class.isAssignableFrom(classToLoad))
								&& (!DistributedResult.class.isAssignableFrom(classToLoad))
								&& (!DistributedTask.class.isAssignableFrom(classToLoad))) {
					LOGGER.log(Level.INFO, "Loaded class with non-default interface: {0}", classToLoad.getName());
				}
				else {
					LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
				}
			}

			// load server binary
			classList = getClassNamesInJar(mCDBContext.getServerBin().getPath());

			for (String classname : classList) {
				System.out.println("Trying to load class: " + classname);
				// finds the DistributedControl instance
				Class classToLoad = child.loadClass(classname);
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
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
		catch (SecurityException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

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
	private void extractCandisDistributedBundle(final File cdbfile, final CDBContext cdbContext) {
		ZipFile zipFile = null;
		String server_binary = null;
		String droid_binary = null;
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
				LOGGER.log(Level.SEVERE, "No server binary given");
				return;
			}
			if (droid_binary == null) {
				LOGGER.log(Level.SEVERE, "No droid binary given");
				return;
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
			while ((lib = p.getProperty(String.format("server.lib.%d.jar", libNumber))) != null) {
				entry = zipFile.getEntry(lib);
				copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(
								new FileOutputStream(cdbContext.getLib(libNumber))));
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
			try {
				if (jarFile != null) {
					jarFile.close();
				}
				LOGGER.log(Level.WARNING, null, "Closed jar input stream");
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
		private int mLibCount;

		/**
		 * Creates CDB context for the given path.
		 *
		 * @param path project path
		 */
		public CDBContext(final String path) {
			mPath = path;
			mDroidBin = new File(mPath, "droid.binary.dex");
			mServerBin = new File(mPath, "server.binary.jar");
			mLibCount = 0;
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

		public File getLib(final int n) {
			return new File(mPath, String.format("server.lib.%d.jar", n));
		}
	}
}
