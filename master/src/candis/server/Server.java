package candis.server;

import candis.common.Settings;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLServerSocketFactory;
//import sun.misc.Signal;
//import sun.misc.SignalHandler;

/**
 *
 * @author enrico
 */
public class Server implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private static final int port = 9999;
	private final ExecutorService tpool;
	private ServerSocket ssocket;
	private boolean doStop = false;
	final DroidManager mDroidManager;
	final ServerCommunicationIO mCommunicationIO;

	/**
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {

		new Server(DroidManager.getInstance());
//		CandisMasterFrame win = new CandisMasterFrame();
	}

	public Server(DroidManager droidmanager) {
		this(droidmanager, new ServerCommunicationIO(droidmanager));
	}

	public Server(DroidManager droidmanager, ServerCommunicationIO scomio) {

		// load properties if available or load default properties
		try {
			Settings.load(new File("settings.properties"));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.WARNING, "settings.properties not found, loading default values");
			Settings.load(Server.class.getResourceAsStream("defaultsettings.properties"));
		}
		mDroidManager = droidmanager;
		mCommunicationIO = scomio;
		// try to load drodmanager
		try {
			mDroidManager.load(new File(Settings.getString("droiddb.file")));
		} catch (FileNotFoundException ex) {
			LOGGER.log(Level.WARNING, "Droid database could not be loaded, initialized empty db");
			mDroidManager.init();
		}
//		final Server server = new Server();
		tpool = Executors.newCachedThreadPool();
		connect();
		LOGGER.log(Level.INFO, "Server terminated");
	}

	public void stop() {
		doStop = true;
	}

	public void runKeytool() {
		// try to exec keytool -- the nice way
		try {
			String tsname = "sometruststore.bks";
			Process proc = Runtime.getRuntime().exec(
							"keytool -import -v -alias clientCert -keystore " + tsname + " -storetype bks -file test.cert -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../bcprov-jdk16-146.jar -storepass changeit -noprompt");
//			Process proc = rt.exec("ls ../");
			BufferedReader buffreader = new BufferedReader(
							new InputStreamReader(proc.getInputStream()));
			String line;
			// Display program output
			LOGGER.log(Level.FINE, "<OUTPUT>");
			while ((line = buffreader.readLine()) != null) {
				LOGGER.log(Level.FINE, line);
			}
			LOGGER.log(Level.FINE, "</OUTPUT>");
			int exitVal = -1;
			try {
				exitVal = proc.waitFor();
			} catch (InterruptedException ex) {
				LOGGER.log(Level.SEVERE, null, ex);
			}
			LOGGER.log(Level.FINE, "Process exitValue: {0}", exitVal);
			if (exitVal == 0) {
				LOGGER.log(Level.INFO, "Truststore sucessfully generated");
			} else {
				LOGGER.log(Level.WARNING, "Creating truststore failed!");
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	public final void connect() {
		Socket socket = null;
		try {
			LOGGER.log(Level.FINE, System.getProperty("ssl.ServerSocketFactory.provider"));

			ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
			ssocket = ssocketFactory.createServerSocket(port);
//			ssocket.setSoTimeout(1000);

			// Listen for connections
			while (!doStop) {
				LOGGER.log(Level.INFO, String.format(
								"Waiting for connection on port %d", ssocket.getLocalPort()));

				socket = ssocket.accept();
				tpool.execute(new Connection(socket, mDroidManager, mCommunicationIO));
			}
			LOGGER.log(Level.INFO, "Server terminated");
			ssocket.close();

		} catch (BindException e) {
			LOGGER.log(Level.SEVERE, String.format(
							"Binding port failed, Address already in use"));
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}

	}

	@Override
	public final void run() {
		connect();
	}
}
