package candis.client.comm;

import android.content.Context;
import android.os.Messenger;
import android.util.Log;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.common.ClassLoaderWrapper;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.QueuedMessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.net.ConnectException;
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
  private final ClassLoaderWrapper mClassLoaderWrapper;
  private QueuedMessageConnection mQueuedMessageConnection;

  public ServerConnection(final X509TrustManager tmanager,
                          final ClassLoaderWrapper cloader,
                          final DroidContext dcontext,
                          final Context context,
                          final Messenger messenger,
                          final JobCenter jobcenter) throws IOException {
    mClassLoaderWrapper = cloader;
    mMessenger = messenger;
    mSecureSocket = new SecureSocket(tmanager);
    mFSM = new ClientStateMachine(this, dcontext, context, messenger, jobcenter);
    isStopped = false;
  }

  public void connect(String host, int port) throws ConnectException {
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
          Thread.sleep(3000);
        }
        catch (InterruptedException ex) {
          LOGGER.log(Level.SEVERE, null, ex);
        }
      }
      // try to connect
      try {
        mSecureSocket.connect(host, port);

        mQueuedMessageConnection = new QueuedMessageConnection(
                mSecureSocket.getSocket(),
                mClassLoaderWrapper);
        success = true;
      }
      // if connect failed, increase connectCounter
      catch (ConnectException ex) {
        connectCounter++;
        LOGGER.severe("Connection to host " + host + " failed!");
      }
      // really unknown exception
      catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
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
   * Thread to handle incoming requests.
   */
  @Override
  public void run() {
    // start message queue
    new Thread(mQueuedMessageConnection).start();

    try {
      mFSM.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
    }
    catch (StateMachineException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

    while (!isStopped) {
      try {
        Message msg = mQueuedMessageConnection.readMessage();
        try {
          if (msg.getData() == null) {
            mFSM.process(((Message) msg).getRequest());
          }
          else if (msg.getRequest() == Instruction.PING) {
            LOGGER.fine("Got PING request, sending PONG");
            sendMessage(Message.create(Instruction.PONG));
          }
          else {
            LOGGER.fine("Got Message: " + msg.getRequest());
            mFSM.process(msg.getRequest(), (Object[]) msg.getData());
          }
        }
        catch (StateMachineException ex) {
          Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
      catch (IOException ex) {
        LOGGER.log(Level.SEVERE, null, ex);
      }
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
