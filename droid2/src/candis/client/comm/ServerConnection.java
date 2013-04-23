package candis.client.comm;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.QueuedMessageConnection;
import candis.distributed.WorkerQueue;
import java.io.IOException;
import java.io.InterruptedIOException;
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

  private static final Logger LOGGER = Logger.getLogger(ServerConnection.class.getName());
  // Minimum reconnect interval: 1 seconds
  private static final int RECONNECT_DELAY_MIN = 1000;
  // Maximum reconnect interval: 60 second
  private static final int RECONNECT_DELAY_MAX = 30000; // TODO: setting
  // interval factor
  private static final double RECONNECT_DELAY_FACTOR = 1.2;
  // Time between subsequent ping messages
  private static final long PING_PERIOD = 5000;
  private SecureSocket mSecureSocket;
  private String mHostname;
  private int mPort;
  private final X509TrustManager mTrustManager;
  private QueuedMessageConnection mQueuedMessageConnection;
  private List<Receiver> mReceivers = new LinkedList<Receiver>();
  private Timer mReconnectTimer = new Timer();
  private Thread mQMCThread;
  private Thread mReceiverThread;
  private final Timer mPingTimer = new Timer();
  private TimerTask mReconnectTimerTask;
  private TimerTask mPingTimerTask;
  private boolean mPongFlag;
  private boolean mConnectEnabled = false;
  private boolean mConnected = false;
  private final Object object = new Object();
  private long mReconnectDelay = RECONNECT_DELAY_MIN;
  private WorkerQueue mWorkerQueue;

  public ServerConnection(String hostname, int port, X509TrustManager trustmanager) {
    mHostname = hostname;
    mPort = port;
    mTrustManager = trustmanager;
    mWorkerQueue = new WorkerQueue();
    new Thread(mWorkerQueue).start();
  }

  public void connect() {
    System.out.println("connect() called");
    if (mConnectEnabled) {
      return;
    }
    mConnectEnabled = true;
    mReconnectDelay = RECONNECT_DELAY_MIN;

    mWorkerQueue.add(new Runnable() {
      public void run() {
        _connect();
      }
    });
  }

  public void disconnect() {
    System.out.println("disconnect() called");
    if (!mConnectEnabled) {
      return;
    }
    mConnectEnabled = false;

    mWorkerQueue.add(new Runnable() {
      public void run() {
        _disconnect();
      }
    });
  }

  private class ConnectRunnable implements Runnable {

    @Override
    public void run() {
      while (true) {
        synchronized (object) {
          try {
            object.wait();
          }
          catch (InterruptedException ex) {
            Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
          }
        }
        System.out.println("mConnectEnabled: " + mConnectEnabled);
        System.out.println("mConnected:      " + mConnected);
        // we were already triggered by someone
        if (mReconnectTimerTask != null) {
          mReconnectTimerTask.cancel();
        }
        // check if we can do something
        if (mConnectEnabled && !mConnected) {
          _connect();
        }
        else if (!mConnectEnabled && mConnected) {
          _disconnect();
        }
      }
    }
  }

  private void _connect() {
    System.out.println("_connect() invoked");
    try {
      mSecureSocket = new SecureSocket(mTrustManager);
      mSecureSocket.connect(mHostname, mPort);

      // TODO: wait for socket?
      mQueuedMessageConnection = new QueuedMessageConnection(mSecureSocket.getSocket());

      // start message queue thread
      mQMCThread = new Thread(mQueuedMessageConnection);
      mQMCThread.setDaemon(true);
      mQMCThread.start();

      // start receiver thread
      mReceiverThread = new Thread(new ServerConnection.MessageReceiver());
      mReceiverThread.start();
      // reset reconnect delay to minimum
      mReconnectDelay = RECONNECT_DELAY_MIN;

      // start PingTimerTask
      LOGGER.info("Starting ping timer...");
      mPongFlag = true;
      mPingTimerTask = new ServerConnection.PingTimerTask();
      mPingTimer.schedule(mPingTimerTask, PING_PERIOD, PING_PERIOD);

      mConnected = true;

      notifyListeners(Status.CONNECTED);
    }
    // connect failed
    catch (IOException ex) {
      LOGGER.warning(ex.toString());
      _disconnect();
      // run timertask
      notifyListeners(Status.DISCONNECTED);
      // calculate new reconnect delay
      if (mReconnectDelay < RECONNECT_DELAY_MAX) {
        mReconnectDelay *= RECONNECT_DELAY_FACTOR;
      }
      if (mReconnectTimerTask != null) {
        mReconnectTimerTask.cancel();
      }
      System.out.println("REstarting connect task...");
      mReconnectTimerTask = new ReconnectTimerTask();
      mReconnectTimer.schedule(mReconnectTimerTask, mReconnectDelay);
    }
  }

  private void _disconnect() {
    System.out.println("_disconnect() invoked");
    // stop conenction
    // stop timer and sender/receiver threads
    if (mPingTimerTask != null) {
      mPingTimerTask.cancel();
    }
    if (mReconnectTimerTask != null) {
      mReconnectTimerTask.cancel();
    }
    if (mReceiverThread != null) {
      mReceiverThread.interrupt();
    }
    if (mQMCThread != null) {
      mQMCThread.interrupt();
    }
    if (mSecureSocket != null) {
      mSecureSocket.close();
    }

    mConnected = false;

    notifyListeners(Status.DISCONNECTED);
  }

  /**
   * Sends Message if connected.
   *
   * @param msg
   */
  public void sendMessage(Message msg) {
    if (mQueuedMessageConnection == null) {
      LOGGER.warning(String.format("Message %s could not be send.", msg.getRequest().toString()));
      return;
    }

    try {
      mQueuedMessageConnection.sendMessage(msg);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }

  private class MessageReceiver implements Runnable {

    /**
     * Thread to handle incoming requests.
     */
    @Override
    public void run() {
      // start message queue
      while (!Thread.interrupted()) {
        try {
          // wait for new message
          Message msg = mQueuedMessageConnection.readMessage();

          if (msg.getRequest() == Instruction.PONG) {
            mPongFlag = true;
            continue;
          }

          // notify listeners
          for (Receiver r : mReceivers) {
            r.OnNewMessage(msg);
          }
        }
        // The thread was interrupted
        catch (InterruptedIOException ex) {
          LOGGER.warning(ex.getMessage());
          mWorkerQueue.add(new Runnable() {
            public void run() {
              _disconnect();
              if (mConnectEnabled) {
                _connect();
              }
            }
          });
          return;
        }
        // The socket was close of any reason
        catch (IOException ex) {
          LOGGER.warning(ex.getMessage());
          mWorkerQueue.add(new Runnable() {
            public void run() {
              _disconnect();
              if (mConnectEnabled) {
                _connect();
              }
            }
          });
          return;
        }
      }
      LOGGER.log(Level.INFO, "[[MessageReceiver THREAD done]]");

      mWorkerQueue.add(new Runnable() {
        public void run() {
          _disconnect();
        }
      });

    }
  }

  private class ReconnectTimerTask extends TimerTask {

    @Override
    public void run() {
      mWorkerQueue.add(new Runnable() {
        public void run() {
          if (mConnectEnabled) {
            _connect();
          }
        }
      });
    }
  }

  private class PingTimerTask extends TimerTask {

    @Override
    public void run() {
      // if pong was received, ok, send new ping
      if (mPongFlag) {
        mPongFlag = false;
        LOGGER.warning("Ping sent...");
        sendMessage(new Message(Instruction.PING));
      }
      // if no pong was received, we assume a timeout and reconnect
      else {
        LOGGER.warning("Pong timed out, restarting connection.");
        cancel();
        synchronized (object) {
          object.notify();
        }
      }
    }
  }

  public enum Status {

    CONNECTED,
    DISCONNECTED
  }

  /**
   * Adds a receiver that will receive messages and connection status updates.
   *
   * @param rec Receiver to add
   */
  public void addReceiver(Receiver rec) {
    if (rec != null) {
      LOGGER.info(String.format("Adding receiver... %s", rec.toString()));
      System.out.println("Adding receiver... " + rec);
      mReceivers.add(rec);
    }
  }

  /**
   * Notify listeners about status update
   *
   * @param status Status to promote
   */
  private void notifyListeners(final Status status) {
    new Thread(new Runnable() {
      public void run() {
        for (Receiver r : mReceivers) {
          r.OnStatusUpdate(status);
        }
      }
    }).start();
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
