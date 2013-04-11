package candis.client.comm;

import candis.common.Message;
import java.io.IOException;
import java.net.ConnectException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 *
 * @author Enrico Joerns
 */
public class ServerConnection {

  private final SecureSocket mSecureSocket;
  private Timer mTimer;
  private long mReconnectDelay;
  private boolean mConnectEnabled = false;
  private String mHostname;
  private int mPort;
  private List<Receiver> receivers = new LinkedList<Receiver>();
  // Minimum reconnect interval: 1 seconds
  private static final int RECONNECT_DELAY_MIN = 1000;
  // Maximum reconnect interval: 60 second
  private static final int RECONNECT_DELAY_MAX = 60000;
  // interval factor
  private static final int RECONNECT_DELAY_FACTOR = 2;

  public enum Status {

    CONNECTED,
    DISCONNECTED
  }

  /**
   *
   */
  public ServerConnection(String hostname, int port, X509TrustManager trustmanager) {
    mHostname = hostname;
    mPort = port;
    mSecureSocket = new SecureSocket(trustmanager);
  }

  public void setHost(String hostname, int port) {
    mHostname = hostname;
    mPort = port;
  }

  /**
   * Connects to the Host.
   * An existing connection will not be changed.
   *
   * If connection fails, reconnect attemps are scheduled automatically
   */
  public void connect() {
    // quit if connecting is already enabled
    if (mConnectEnabled) {
      return;
    }

    // init (re)connection scheduler
    mReconnectDelay = RECONNECT_DELAY_MIN;
    mConnectEnabled = true;
    mTimer = new Timer();
    mTimer.schedule(new ConnectTask(), 0);
  }

  /**
   * Reconnects to the Host.
   * If the Host was not connected before, no reconnect is performed.
   * An existing connection will be disconnected.
   */
  public void reconnect() {
    if (!mConnectEnabled) {
      return;
    }
    disconnect();
    connect();
  }

  /**
   * Disconnects from the Host.
   */
  public void disconnect() {
    // quit if connecting is already disable
    if (!mConnectEnabled) {
      return;
    }

    // stop conenction
    mConnectEnabled = false;
    mTimer.cancel();
    new Thread(new Runnable() {
      public void run() {
        mSecureSocket.close();
        notifyListeners(Status.DISCONNECTED);
      }
    }).start();
  }

  /**
   * Sends Message if connected.
   *
   * @param msg
   */
  public void sendMsg(Message msg) {
  }

  public void addReceiver(Receiver rec) {
    if (rec != null) {
      receivers.add(rec);
    }
  }

  /**
   * Task scheduled peridically to test connection
   */
  private class ConnectTask extends TimerTask {

    @Override
    public void run() {
      if ((mConnectEnabled) && (!mSecureSocket.isConnected())) {
        // try to connect to host
        try {
          mSecureSocket.connect(mHostname, mPort);
          notifyListeners(Status.CONNECTED);
        }
        // connect failed
        catch (IOException ex) {
          Logger.getLogger(ServerConnection.class.getName())
                  .log(Level.SEVERE, ex.getMessage());
          notifyListeners(Status.DISCONNECTED);
          if (mReconnectDelay < RECONNECT_DELAY_MAX) {
            mReconnectDelay *= RECONNECT_DELAY_FACTOR;
          }
          mTimer = new Timer();
          mTimer.schedule(new ConnectTask(), mReconnectDelay);
        }
      }
    }
  }

  /**
   * Notify listeners about status update
   *
   * @param status Status to promote
   */
  private void notifyListeners(Status status) {
    for (Receiver r : receivers) {
      r.OnStatusUpdate(status);
    }
  }

  /*
   * 
   */
  public interface Receiver {

    /**
     * Invoked when new message was received.
     *
     * @param msg
     */
    public abstract void OnNewMessage(Message msg);

    /**
     * Invoked when connection status changes.
     *
     * @param status
     */
    public abstract void OnStatusUpdate(Status status);
  }
}
