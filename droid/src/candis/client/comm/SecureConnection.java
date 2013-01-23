package candis.client.comm;

import java.io.IOException;
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

  private static final Logger LOGGER = Logger.getLogger(SecureConnection.class.getName());
  private Socket socket = null;
  private boolean mConnected;
  private X509TrustManager mTrustManager;

  /**
   * Creates new SecureConnection.
   *
   * @param truststore_R truststore file to be used
   */
  public SecureConnection(final X509TrustManager tstore) {
    mTrustManager = tstore;
  }

  /**
   * Creates a socket.
   *
   * @param host remote host address to connect with
   * @param port remote port number to connect to
   * @return Socket if successfull or null if failed.
   */
  public void connect(final String host, final int port) throws IOException {

    if (mConnected) {
      LOGGER.log(Level.WARNING, "Already connected");
      return;
    }

    LOGGER.log(Level.INFO, "Starting connection");

    SSLContext context;
    try {
      context = SSLContext.getInstance("TLS");
      context.init(null, new TrustManager[]{mTrustManager}, null);
    }
    catch (NoSuchAlgorithmException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return;
    }
    catch (KeyManagementException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return;
    }
    catch (Exception ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      return;
    }
    SSLSocketFactory sf = context.getSocketFactory();

    try {
      socket = sf.createSocket(host, port);
      LOGGER.log(Level.INFO, String.format(
              "Connected to %s:%d", socket.getInetAddress(), socket.getPort()));
    }
    catch (UnknownHostException ex) {
      LOGGER.log(Level.SEVERE, "UnknownHostException");
      return;
    }

    mConnected = true;
  }

  /**
   *
   * @param address
   * @param port
   * @throws IOException
   */
  public void connect(InetAddress address, int port) throws IOException {
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
    }
    catch (IOException ex) {
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

  /**
   *
   * @return
   */
  public boolean isConnected() {
    return mConnected;
  }
}