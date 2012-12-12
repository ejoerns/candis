package candis.server;

import candis.common.fsm.StateMachineException;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedControl;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.DroidData;
import candis.distributed.Scheduler;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO, Runnable {

	protected Scheduler scheduler;
	protected DistributedControl distributedControl;
	protected final DroidManager mDroidManager;
	private Thread queueThread;
	private static final Logger LOGGER = Logger.getLogger(ServerCommunicationIO.class.getName());
	private final List<Runnable> comIOQueue = new LinkedList<Runnable>();
	private byte[] mServerBinary;
	private byte[] mDroidBinary;

	public ServerCommunicationIO(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setDistributedControl(DistributedControl dc) {
		if (scheduler == null || scheduler.isDone()) {
			distributedControl = dc;
			scheduler = dc.initScheduler();
			scheduler.setCommunicationIO(this);
			queueThread = new Thread(this);
			queueThread.start();
		}


	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

	public byte[] getDroidBinary() {
		return mDroidBinary;
	}

	public byte[] getServerBinary() {
		return mServerBinary;
	}

	@Override
	public void startJob(String id, DistributedParameter param) {
		Connection d = getDroidConnection(id);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_JOB, param);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	// Called by Scheduler.
	@Override
	public void sendBinary(String droidID) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_BINARY, getDroidBinary());
		}
		catch (StateMachineException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void sendInitialParameter(String droidID, DistributedParameter parameter) {
		Connection d = getDroidConnection(droidID);
		try {
			d.getStateMachine().process(ServerStateMachine.ServerTrans.SEND_INITAL, parameter);
		}
		catch (StateMachineException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void stopJob(final String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	private boolean isQueueEmpty() {
		synchronized (comIOQueue) {
			return comIOQueue.isEmpty();
		}
	}

	@Override
	public void run() {
		try {
			while (!(isQueueEmpty() && scheduler.isDone())) {

				while (!isQueueEmpty()) {
					Runnable task;
					synchronized (comIOQueue) {
						task = comIOQueue.remove(0);

					}
					task.run();
				}
				if (!scheduler.isDone()) {
					synchronized (comIOQueue) {
						if (comIOQueue.isEmpty()) {
							comIOQueue.wait();
						}
					}
				}

			}
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		distributedControl.schedulerDone();
		LOGGER.log(Level.INFO, "CommunicationIO done");
	}

	@Override
	public void join() throws InterruptedException {
		queueThread.join();
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	protected void addToQueue(Runnable task) {

		synchronized (comIOQueue) {
			comIOQueue.add(task);
			comIOQueue.notify();
		}
	}

	public void startScheduler() {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.start();
			}
		});

	}

	public void onJobDone(final String droidID, final DistributedResult result) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onJobDone(droidID, result);
			}
		});

	}

	public void onDroidConnected(final String droidID, final Connection connection) {
		mDroidManager.connectDroid(droidID, connection);
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onNewDroid(droidID);
			}
		});
	}

	public void onBinarySent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onBinaryRecieved(droidID);
			}
		});
	}

	public void onInitalParameterSent(final String droidID) {
		addToQueue(new Runnable() {
			@Override
			public void run() {
				scheduler.onInitParameterRecieved(droidID);
			}
		});
	}
	private DistributedControl mLoadedDistributedControl;
	private DistributedParameter mLoadedDistributedInitialParameter;
	private DistributedParameter mLoadedDistributedParameter;
	private DistributedResult mLoadedDistributedResult;
	private DistributedTask mLoadedDistributedTask;

	public void loadCandisDistributedBundle(final File cdbfile) {
		final String projectPath = cdbfile.getName().substring(0, cdbfile.getName().lastIndexOf('.'));
		final CDBContext cdbContext = new CDBContext(projectPath);
		extractCandisDistributedBundle(cdbfile, cdbContext);

		List<String> classList = null;

		URLClassLoader child;
		try {

			child = new URLClassLoader(
							new URL[]{cdbContext.getLib(0).toURI().toURL(), cdbContext.getServerBin().toURI().toURL()},
							this.getClass().getClassLoader());
			
			// load lib 0
			// TODO: allow for multiple libs
			classList = getClassNamesInJar(cdbContext.getLib(0).getPath());
			
			for (String classname : classList) {
				// finds the DistributedControl instance
				Class classToLoad = child.loadClass(classname);
				if ((!DistributedParameter.class.isAssignableFrom(classToLoad))
					&& (!DistributedResult.class.isAssignableFrom(classToLoad))
					&& (!DistributedTask.class.isAssignableFrom(classToLoad))) {
					LOGGER.log(Level.INFO, "Loaded class with non-default interface: {0}", classToLoad.getName());
				} else {
					LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
				}
			}

			// load server binary
			classList = getClassNamesInJar(cdbContext.getServerBin().getPath());
			
			for (String classname : classList) {
				System.out.println("Trying to load class: " + classname);
				// finds the DistributedControl instance
				Class classToLoad = child.loadClass(classname);
				if (DistributedControl.class.isAssignableFrom(classToLoad)) {
					LOGGER.log(Level.FINE, "Loaded class : {0}", classToLoad.getName());
					try {
						DistributedControl obj = (DistributedControl) classToLoad.newInstance();
						obj.initScheduler();
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
	 * @param file Name of cdb-file
	 * @return Project directory if succeede, otherwise null
	 */
	private void extractCandisDistributedBundle(final File cdbfile, final CDBContext cdbContext) {
		ZipFile zipFile = null;
//		String projectDir = null;
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

	public void listLoadedClasses(ClassLoader byClassLoader) {
		Class clKlass = byClassLoader.getClass();
		System.out.println("Classloader: " + clKlass.getCanonicalName());
		while (clKlass != java.lang.ClassLoader.class) {
			clKlass = clKlass.getSuperclass();
		}
		try {
			java.lang.reflect.Field fldClasses = clKlass
							.getDeclaredField("classes");
			fldClasses.setAccessible(true);
			Vector classes = (Vector) fldClasses.get(byClassLoader);
			for (Iterator iter = classes.iterator(); iter.hasNext();) {
				System.out.println("   Loaded " + iter.next());
			}
		}
		catch (SecurityException e) {
			e.printStackTrace();
		}
		catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
		catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Finds all class names in a jar file.
	 *
	 * @param jarName Jar file to search in
	 * @return List of all full class names
	 */
	public static List<String> getClassNamesInJar(String jarName) {
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
		public CDBContext(String path) {
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

		public File getLib(int n) {
			return new File(mPath, String.format("server.lib.%d.jar", n));
		}
	}
}
