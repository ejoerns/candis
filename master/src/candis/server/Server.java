package candis.server;

import candis.common.Settings;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;

/**
 *
 * @author enrico
 */
public class Server implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private static final int mPort = 9999;
	private final ExecutorService mExecutorService;
	private final DroidManager mDroidManager;
	private final JobDistributionIOServer mCommunicationIO;
	private ServerSocket ssocket;
	private boolean mDoStop = false;
//	private final ClassLoaderWrapper mClassLoaderWrapper;

	/**
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {
		try {
			new Server(DroidManager.getInstance()).connect();
		}
		catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public Server(final DroidManager droidmanager) throws IOException {
		this(droidmanager, new JobDistributionIOServer(droidmanager));
	}

	public Server(
					final DroidManager droidmanager,
					final JobDistributionIOServer scomio) throws IOException {

		// load properties if available or load default properties
		try {
			Settings.load(new File("settings.properties"));
		}
		catch (FileNotFoundException ex) {
			LOGGER.log(Level.WARNING, "settings.properties not found, loading default values");
			Settings.load(Server.class.getResourceAsStream("defaultsettings.properties"));
		}
		mDroidManager = droidmanager;
//		mClassLoaderWrapper = clw;
		mCommunicationIO = scomio;
		// try to load drodmanager
		try {
			mDroidManager.load(new File(Settings.getString("droiddb.file")));
		}
		catch (FileNotFoundException ex) {
			LOGGER.log(Level.WARNING, "Droid database could not be loaded, initialized empty db");
			mDroidManager.init();
		}

		mExecutorService = Executors.newCachedThreadPool();
	}

	public void stop() {
		mDoStop = true;
	}

	public final void connect() throws IOException {
		Socket socket = null;

		LOGGER.log(Level.FINE, System.getProperty("ssl.ServerSocketFactory.provider"));

		ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
		ssocket = ssocketFactory.createServerSocket(mPort);
//			ssocket.setSoTimeout(1000);

		// Listen for connections
		while (!mDoStop) {
			LOGGER.log(Level.INFO, String.format(
							"Waiting for connection on port %d", ssocket.getLocalPort()));

			socket = ssocket.accept();
			mExecutorService.execute(new Connection(socket, mDroidManager, mCommunicationIO));
		}
		LOGGER.log(Level.INFO, "Server terminated");
		ssocket.close();
	}

	@Override
	public final void run() {
		try {
			connect();
		}
		catch (IOException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
