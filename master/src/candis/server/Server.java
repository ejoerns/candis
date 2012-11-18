package candis.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ServerSocketFactory;
import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
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
	public static void main(String[] args) {
//		ServerFrame win = new ServerFrame();

		final Server server = new Server();
		server.connect();
		LOGGER.log(Level.INFO, "Ende im Gelände");
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

	public void connect() {
		tpool = Executors.newCachedThreadPool();
		try {
			LOGGER.log(Level.FINE, System.getProperty("ssl.ServerSocketFactory.provider"));

			ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
			ssocket = ssocketFactory.createServerSocket(port);
//			ssocket.setSoTimeout(1000);

			// Listen for connections
			while (!doStop) {
				LOGGER.log(Level.INFO, "Waiting for connection");

				try {
					Socket socket = ssocket.accept();
					// Start new server thread in thread pool
					tpool.execute(new Connection(socket));
				} catch (SocketTimeoutException e) {
//					LOGGER.log(Level.SEVERE, "SocketTimeoutException");
				}
			}
			LOGGER.log(Level.INFO, "Server terminated");
			ssocket.close();

		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, null, e);
		}

	}
}
