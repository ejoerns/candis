package candis.server;

import candis.common.RandomID;
import candis.distributed.droid.StaticProfile;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
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
public class Server {

	private static final Logger LOGGER = Logger.getLogger(Server.class.getName());
	private static ExecutorService tpool;
	private static final int port = 9999;
	private ServerSocket ssocket;
	private boolean doStop = false;

	/**
	 * @param args the command line arguments
	 */
	public static void main(final String[] args) {
//		ServerFrame win = new ServerFrame();

		DroidManager manager = DroidManager.getInstance();
		manager.addDroid(
						RandomID.init("foo"),
						new StaticProfile(1024, 2, 4711));
		manager.addDroid(
						RandomID.init("foo"),
						new StaticProfile(1024, 2, 4711));
		try {
			DroidManager.writeToFile(new File("/home/enrico/droiddb"), manager);
			Map<String, DroidData> map;
			map = DroidManager.readFromFile(new File("/home/enrico/droiddb"));
			for (Map.Entry<String, DroidData> e : map.entrySet()) {
				System.out.println("Key: " + e.getKey());
				System.out.println("blacklist: " + e.getValue().getBlacklist() + ", mem:" + e.getValue().getProfile().memoryMB);
			}
		} catch (FileNotFoundException ex) {
			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
		}

		if (true) {
			return;
		}

		final Server server = new Server();
		tpool = Executors.newCachedThreadPool();
		server.connect();
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
				LOGGER.log(Level.INFO, String.format("Waiting for connection on port %d", ssocket.getLocalPort()));

				socket = ssocket.accept();
				tpool.execute(new Connection(socket));
			}
			LOGGER.log(Level.INFO, "Server terminated");
			ssocket.close();

		} catch (BindException e) {
			LOGGER.log(Level.SEVERE, "Binding port failed, Address already in use");
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}

	}
}
