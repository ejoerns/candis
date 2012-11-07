/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.server;

import candis.server.gui.ServerFrame;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
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

	private static final Logger logger = Logger.getLogger(Server.class.getName());
	private static ExecutorService tpool;
	private static final int port = 9999;
	private ServerSocket ssocket;
	private boolean doStop = false;

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
//		ServerFrame win = new ServerFrame();

//		System.out.print("Running keytool...");
//		try {
//			Runtime rt = Runtime.getRuntime();
//			Process proc = rt.exec("keytool -import -v -alias clientCert -keystore sometruststore.bks -storetype bks -file ../../../test_server/test.cert -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath \"../../../bcprov-jdk16-146.jar\"");
//			int exitVal = proc.exitValue();
//			System.out.println("Process exitValue: " + exitVal);
//		} catch (IOException ex) {
//			Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
//		}
//		System.out.println("[DONE]");

		// try to exec keytool -- the nice way
		try {
			Runtime rt = Runtime.getRuntime();
			String tsname = "sometruststore.bks";
			Process proc = rt.exec("keytool -import -v -alias clientCert -keystore " + tsname + " -storetype bks -file test.cert -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath ../bcprov-jdk16-146.jar -storepass changeit -noprompt");
//			Process proc = rt.exec("ls ../");
			InputStream stdin = proc.getInputStream();
			InputStreamReader isr = new InputStreamReader(stdin);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			logger.log(Level.FINE, "<OUTPUT>");
			while ((line = br.readLine()) != null) {
				logger.log(Level.FINE, line);
			}
			logger.log(Level.FINE, "</OUTPUT>");
			int exitVal = proc.waitFor();
			logger.log(Level.FINE, "Process exitValue: {0}", exitVal);
			if (exitVal == 0) {
				logger.log(Level.INFO, "Truststore sucessfully generated");
			} else {
				logger.log(Level.WARNING, "Creating truststore failed!");
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}

		final Server server = new Server();
//		Runtime.getRuntime().addShutdownHook(new Thread() {
//
//			@Override
//			public void run() {
//				System.out.println("aa");
//				server.stop();
//			}
//		});

//		SignalHandler signalHandler = new SignalHandler() {
//
//			public void handle(Signal signal) {
//				server.stop();
//			}
//		};
//		Signal.handle(new Signal("TERM"), signalHandler);
//		Signal.handle(new Signal("INT"), signalHandler);

		server.connect();
		logger.log(Level.INFO, "Ende im Gelände");
	}

	public void stop() {
		doStop = true;
	}

	public void connect() {
		tpool = Executors.newCachedThreadPool();
		try {
			logger.log(Level.FINE, System.getProperty("ssl.ServerSocketFactory.provider"));

			ServerSocketFactory ssocketFactory = SSLServerSocketFactory.getDefault();
			ssocket = ssocketFactory.createServerSocket(port);
			ssocket.setSoTimeout(10);

			// Listen for connections
			while (!doStop) {
				logger.log(Level.INFO, "Waiting for connection");

				while (!doStop) {

					try {
						Socket socket = ssocket.accept();
						// Start new server thread in thread pool
						tpool.execute(new Connection(socket));
					} catch (SocketTimeoutException e) {
					}
				}
			}
			ssocket.close();

		} catch (IOException e) {
			logger.log(Level.SEVERE, null, e);
		}


	}
}
