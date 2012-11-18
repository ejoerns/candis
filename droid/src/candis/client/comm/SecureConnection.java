package candis.client.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
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
public final class SecureConnection {// TODO: maybe extend SocketImpl later...

	private static final String TAG = "SecureConnection";
	private static final Logger LOGGER = Logger.getLogger(TAG);
	private Socket socket = null;
	/// Host to connect to
	private String host;
	/// Port to connect to
	private int port;
	private boolean connected;
	private OutputStream mOutstream;
	private InputStream mInstream;
	private X509TrustManager tstore;

	/**
	 * Creates new SecureConnection.
	 *
	 * @param truststore_R truststore file to be used
	 */
	public SecureConnection(final X509TrustManager tstore) {
		this.tstore = tstore;
	}

	/**
	 * Creates a socket.
	 *
	 * @param host remote host address to connect with
	 * @param port remote port number to connect to
	 * @return Socket if successfull or null if failed.
	 */
	public void connect(final String host, final int port) {
		this.host = host;
		this.port = port;

		if (connected) {
			LOGGER.log(Level.WARNING, "Already connected");
			return;
		}

		LOGGER.log(Level.FINE, "Starting connection");

		SSLContext context = null;
		try {
			context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[]{tstore}, null);
		} catch (NoSuchAlgorithmException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			return;
		} catch (KeyManagementException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			return;
		} catch (Exception ex) {
			LOGGER.log(Level.SEVERE, null, ex);
			return;
		}
		SSLSocketFactory sf = context.getSocketFactory();

		LOGGER.log(Level.FINE, "Got SSLSocketFactory");
		try {
			socket = sf.createSocket(host, port);
		} catch (UnknownHostException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		LOGGER.log(Level.INFO, String.format(
						"Connected to %s:%d", socket.getInetAddress(), socket.getPort()));

		try {
			mOutstream = socket.getOutputStream();
			Thread.sleep(500);
			mInstream = socket.getInputStream();
		} catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed creating input/output streams");
		}

		connected = true;
	}

	public void connect(InetAddress address, int port) {
		connect(address.getHostName(), port);
	}

	/**
	 * Disconnects the socket.
	 */
	public void close() {
		LOGGER.log(Level.INFO, "Closing socket...");
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException ex) {
			LOGGER.log(Level.SEVERE, "Failed to close socket");
		}
	}

	/**
	 *
	 * @return Socket
	 */
	public Socket getSocket() {
		return socket;
	}

	public boolean isConnected() {
		return connected;
	}

	public InputStream getInputStream() throws IOException {
		return mInstream;
	}

	public OutputStream getOutputStream() throws IOException {
		return mOutstream;
	}
}
