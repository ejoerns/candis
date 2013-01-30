package candis.client.comm;

import android.content.Context;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.client.service.BackgroundService;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.QueuedMessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.net.ssl.X509TrustManager;

/**
 * Processes incoming data from socket.
 *
 * Designed to be tun in a sepearte thread.
 *
 * @author Enrico Joerns
 */
public class ServerConnection implements Runnable {

  private static final String TAG = ServerConnection.class.getName();
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private static final int MAX_CONNECT_TRIES = 5;
  private static final int RECONNECT_DELAY = 3000;
  private boolean isStopped;
  private final FSM mFSM;
  private final Messenger mMessenger;
  private final SecureSocket mSecureSocket;
  private QueuedMessageConnection mQueuedMessageConnection;
  Thread mQMCThread;

  public ServerConnection(final X509TrustManager tmanager,
                          final DroidContext dcontext,
                          final Context context,
                          final Messenger messenger,
                          final JobCenter jobcenter) throws IOException {
    mMessenger = messenger;
    mSecureSocket = new SecureSocket(tmanager);
    mFSM = new ClientStateMachine(this, dcontext, context, messenger, jobcenter);
    isStopped = false;
  }

  public void connect(String host, int port) throws ConnectException, IOException {
    boolean success = false;
    int connectCounter = 0;
    // try to connect for some iterations...
    do {
      // if maximum number of reconnects is reached, give up
      if (connectCounter >= MAX_CONNECT_TRIES) {
        throw new ConnectException(String.format(
                "Gave up connecting after %d failed attempts!", connectCounter));
      }
      // if previous connection failed, first wait 3 seconds
      else if (connectCounter > 0) {
        try {
          Thread.sleep(RECONNECT_DELAY);
        }
        catch (InterruptedException ex) {
          mSecureSocket.close();
          throw new ConnectException("Connection thread interrupted");
        }
      }
      // try to connect
      try {
        mSecureSocket.connect(host, port);

        mQueuedMessageConnection = new QueuedMessageConnection(mSecureSocket.getSocket());
        success = true;
      }
      // if connect failed, increase connectCounter
      catch (ConnectException ex) {
        connectCounter++;
        LOGGER.severe("Connection to host " + host + " failed!");
      }
    }
    while (!success); // && (connectCounter < 3));
    mFSM.init();
  }

  /**
   *
   * @return
   */
  public FSM getFSM() {
    return mFSM;
  }

  /**
   *
   * @return
   */
  public Socket getSocket() {
    return mSecureSocket.getSocket();
  }

  /**
   * Thread to handle incoming requests.
   */
  @Override
  public void run() {
    // start message queue

    mQMCThread = new Thread(mQueuedMessageConnection);
    mQMCThread.setDaemon(true);
    mQMCThread.start();

    mFSM.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);

    while ((!isStopped) && (!Thread.interrupted())) {
      try {
        Message msg = mQueuedMessageConnection.readMessage();
        try {
          if (msg.getData() == null) {
            mFSM.process(((Message) msg).getRequest());
          }
          else if (msg.getRequest() == Instruction.PING) {
            Log.w(TAG, "Got PING request, sending PONG");
            sendMessage(Message.create(Instruction.PONG));
          }
          else {
            Log.w(TAG, "Got Message: " + msg.getRequest());
            mFSM.process(msg.getRequest(), (Object[]) msg.getData());
          }
        }
        catch (StateMachineException ex) {
          Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      // The thread was interrupted
      catch (InterruptedIOException ex) {
        closeConnection(ex);
      }
      // The socket was close of any reason
      catch (SocketException ex) {
        closeConnection(ex);
      }
      catch (IOException ex) {
        closeConnection(ex);
      }
    }
    Log.i(TAG, "[[ServerConnection THREAD done]]");
  }

  private void closeConnection(Exception ex) {
    LOGGER.warning(ex.getMessage());
    isStopped = true;
    mQMCThread.interrupt();
    try {
      mMessenger.send(android.os.Message.obtain(null, BackgroundService.DISCONNECTED));
    }
    catch (RemoteException ex1) {
      LOGGER.log(Level.SEVERE, null, ex1);
    }
  }

  /**
   * Sends message (buffered).
   *
   * @param msg
   */
  public void sendMessage(final Message msg) {
    try {
      mQueuedMessageConnection.sendMessage(msg);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
}
