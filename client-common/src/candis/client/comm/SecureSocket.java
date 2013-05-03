package candis.client.comm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
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
public final class SecureSocket {// TODO: maybe extend SocketImpl later...

  private static final Logger LOGGER = Logger.getLogger(SecureSocket.class.getName());
  private Socket mSocket = null;
  private boolean mConnected;
  private X509TrustManager mTrustManager;

  /**
   * Creates new SecureSocket with the specified truststore.
   *
   * @param truststore_R truststore file to be used
   */
  public SecureSocket(final X509TrustManager tstore) {
    mTrustManager = tstore;
  }

  /**
   * Connects socket to server.
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

    SSLSocketFactory sf = context.getSocketFactory();

    mSocket = sf.createSocket(host, port);
    LOGGER.log(Level.INFO, String.format(
            "Connected to %s:%d on port %d", mSocket.getInetAddress(), mSocket.getPort(), mSocket.getLocalPort()));

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
      if (mSocket != null) {
        mSocket.close();
        mConnected = false;
        LOGGER.log(Level.INFO, "done.");
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
    return mSocket;
  }

  /**
   *
   * @return
   */
  public boolean isConnected() {
    return mConnected;
  }
}