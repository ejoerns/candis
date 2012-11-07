/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.client.comm;

import android.util.Log;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * SSL/TLS based secure connection.
 *
 *
 *
 * @author enrico
 */
public class SecureConnection implements Runnable {

	private static final String TAG = "SecureConnection";
	private static final Logger logger = Logger.getLogger(TAG);
	private Socket socket = null;
	/// Host to connect to
	private String host;
	/// Port to connect to
	private int port;
	private boolean connected;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private X509TrustManager tstore;
//	private boolean is_onnected;

	/**
	 *
	 * @param host Host address to connect with
	 * @param port port number to connect to
	 * @param truststore_R truststore file to be used
	 */
	public SecureConnection(String host, int port, X509TrustManager tstore) {
		this.host = host;
		this.port = port;
		this.tstore = tstore;
	}

	public void run() {
		connect();
	}

	/**
	 * Creates a socket.
	 *
	 * @return Socket if successfull or null if failed.
	 */
	public void connect() {

		if (connected) {
			logger.log(Level.WARNING, "Already connected");
			return;
		}

		logger.log(Level.FINE, "Starting connection");

		SSLContext context = null;
		try {
			context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[]{tstore}, null);
		} catch (NoSuchAlgorithmException ex) {
			logger.log(Level.SEVERE, null, ex);
			return;
		} catch (KeyManagementException ex) {
			logger.log(Level.SEVERE, null, ex);
			return;
		} catch (Exception ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		SSLSocketFactory sf = context.getSocketFactory();

		logger.log(Level.FINE, "Got SSLSocketFactory");
		try {
			socket = sf.createSocket(host, port);
		} catch (UnknownHostException ex) {
			logger.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

		logger.log(Level.INFO, "Connected to {0}:{1}",
						new Object[]{socket.getInetAddress(), socket.getPort()});

		logger.log(Level.FINE, "Creating input/output streams");
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed creating input/output streams");
		}

		connected = true;
		logger.log(Level.FINE, "Now Connected!");
	}

	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed to close socket");
		}
	}

	public Socket getSocket() {
		return socket;
	}

	public Object readObject() throws IOException {
		Object rec = null;

		if (ois.available() > 0) {
			try {
				rec = ois.readObject();
			} catch (OptionalDataException ex) {
				logger.log(Level.SEVERE, null, ex);
			} catch (ClassNotFoundException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
		} else {
			logger.log(Level.FINE, "No data");
		}

		return rec;
	}

	public void writeObject(Object data) throws IOException {
		oos.writeObject(data);
	}

	public boolean isConnected() {
		return connected;
	}
}
