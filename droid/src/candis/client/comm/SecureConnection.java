package candis.client.comm;

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
 * @author Enrico Joerns
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
	private boolean isStopped = false;

	/**
	 * Creates new SecureConnection.
	 *
	 * @param host Host address to connect with
	 * @param port port number to connect to
	 * @param truststore_R truststore file to be used
	 */
	public SecureConnection(final String host, final int port, final X509TrustManager tstore) {
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
			return;
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

		logger.log(Level.INFO, String.format(
						"Connected to %s:%d", socket.getInetAddress(), socket.getPort()));

		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			Thread.sleep(500);
			ois = new ObjectInputStream(socket.getInputStream());
			logger.log(Level.FINE, "Input/output streams created");
		} catch (InterruptedException ex) {
			Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed creating input/output streams");
		}

		connected = true;
	}

	/**
	 * Disconnects the socket.
	 */
	public void disconnect() {
		logger.log(Level.INFO, "Closing socket...");
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Failed to close socket");
		}
	}

	/**
	 *
	 * @return Socket
	 */
	public Socket getSocket() {
		return socket;
	}

	/**
	 * Reads object from socket.
	 *
	 * @return
	 * @throws IOException
	 */
	public Object readObject() throws IOException {
		Object rec = null;
//		if ((ois == null) && (!isStopped)) {
//			isStopped = true;
//			ois = new ObjectInputStream(socket.getInputStream());
//		}
		if (ois == null) {
			logger.warning("ois is null");
			return null;
		}

//		if (ois.available() > 0) {
			try {
				rec = ois.readObject();
			} catch (OptionalDataException ex) {
				logger.log(Level.SEVERE, null, ex);
			} catch (ClassNotFoundException ex) {
				logger.log(Level.SEVERE, null, ex);
			}
//		} else {
//			logger.log(Level.FINE, "No data");
//		}

		return rec;
	}

	/**
	 * Writes object to socket.
	 *
	 * @param data
	 * @throws IOException
	 */
	public void writeObject(final Object data) throws IOException {
		logger.log(Level.FINE, "writeObject() called " + data.toString());
		oos.writeObject(data);
	}

	public boolean isConnected() {
		return connected;
	}
}
