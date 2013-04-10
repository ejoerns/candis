package candis.client.comm;

import candis.common.Message;
import java.io.File;
import java.io.IOException;
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
  private File mTsfile;
  private String mHostname;
  private int mPort;
  private TimerTask mConnectTask;
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
    mTimer = new Timer();
    mConnectTask = new ConnectTask();
    mSecureSocket = new SecureSocket(trustmanager);
  }

  public void setHost(String hostname, int port) {
    mHostname = hostname;
    mPort = port;
  }

  public void start() {
    mReconnectDelay = RECONNECT_DELAY_MIN;
  }

  public void connect() {
    mConnectEnabled = true;
    if (mSecureSocket.isConnected()) {
      return;
    }
    mTimer.cancel();
    mTimer = new Timer();
    mTimer.schedule(new ConnectTask(), 0);
  }

  public void disconnect() {
    mConnectEnabled = false;
    mTimer.cancel();
    mSecureSocket.close();
  }

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
        try {
          mSecureSocket.connect(mHostname, mPort);
        }
        // connect failed
        catch (IOException ex) {
          Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
          if (mReconnectDelay < RECONNECT_DELAY_MAX) {
            mReconnectDelay *= RECONNECT_DELAY_FACTOR;
          }
          mTimer = new Timer();
          mTimer.schedule(new ConnectTask(), mReconnectDelay);
        }
      }
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

    /**
     * Invoked when server cert needs to be checked.
     *
     * @param cert Cert that needs to be checked.
     */
    public abstract void OnCheckServerCert(X509Certificate cert);
  }
}
