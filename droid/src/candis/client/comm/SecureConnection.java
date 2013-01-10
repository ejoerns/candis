package candis.client.comm;

import candis.common.Message;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
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
  private boolean mConnected;
  private ObjectOutputStream mObjOutstream;
  private InputStream mInstream;
  private X509TrustManager mTrustManager;
  private SendHandler mSendHandler;

  /**
   * Creates new SecureConnection.
   *
   * @param truststore_R truststore file to be used
   */
  public SecureConnection(final X509TrustManager tstore) {
    mTrustManager = tstore;
    mSendHandler = new SendHandler();
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

    try {
      mObjOutstream = new ObjectOutputStream(socket.getOutputStream());
      Thread.sleep(500);
      mInstream = socket.getInputStream();
    }
    catch (InterruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, "Failed creating input/output streams");
    }

    // start message worker queue;
    new Thread(mSendHandler).start();
    mConnected = true;
  }

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

  public boolean isConnected() {
    return mConnected;
  }

  public InputStream getInputStream() throws IOException {
    return mInstream;
  }

  /**
   * Sends data in new thread
   *
   * @todo Replace with send qeue
   * @param msg
   */
  public void sendMessage(final Message msg) {
    mSendHandler.addToQueue(msg);
  }

  private class SendHandler implements Runnable {

    private final List<Message> mMessageQueue = new LinkedList<Message>();
    private boolean mStop = false;// TODO: use? :)

    private boolean isQueueEmpty() {
      synchronized (mMessageQueue) {
        return mMessageQueue.isEmpty();
      }
    }

    @Override
    public void run() {
      try {
        while (!(isQueueEmpty() && (mStop))) {

          while (!isQueueEmpty()) {
            synchronized (mMessageQueue) {
              // send it
              try {
                mObjOutstream.writeObject(mMessageQueue.remove(0));
                mObjOutstream.flush();
              }
              catch (IOException ex) {
                Logger.getLogger(SecureConnection.class.getName()).log(Level.SEVERE, null, ex);
              }
            }
          }
          if (!mStop) {
            synchronized (mMessageQueue) {
              if (mMessageQueue.isEmpty()) {
                mMessageQueue.wait();
              }
            }
          }
        }
      }
      catch (InterruptedException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }

    public void addToQueue(Message msg) {
      synchronized (mMessageQueue) {
        mMessageQueue.add(msg);
        mMessageQueue.notify();
      }
    }
  }
}