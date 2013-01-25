package candis.client.comm;

import android.content.Context;
import android.os.Messenger;
import android.support.v4.app.FragmentManager;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.common.ClassLoaderWrapper;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.common.QueuedMessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

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
  private boolean isStopped;
  private final FSM mFSM;
  private final Messenger mMessenger;
  private final ClassLoaderWrapper mClassLoaderWrapper;
  private final QueuedMessageConnection mMessageConnection;
  
  public ServerConnection(Socket socket,
                          final ClassLoaderWrapper cl,
                          final DroidContext dcontext,
                          final Context context,
                          final Messenger messenger,
                          final FragmentManager fragmanager,
                          final JobCenter jobcenter) throws IOException {
    mClassLoaderWrapper = cl;
    mMessenger = messenger;
    mMessageConnection = new QueuedMessageConnection(socket, mClassLoaderWrapper);
    mFSM = new ClientStateMachine(this, dcontext, context, messenger, fragmanager, jobcenter);
    mFSM.init();
    isStopped = false;
  }

  // TODO: better hack?
  public MessageConnection getMessageConnection() {
    return mMessageConnection;
  }
  
  public FSM getFSM() {
    return mFSM;
  }

  /**
   * Thread to handle incoming requests.
   */
  @Override
  public void run() {
    new Thread(mMessageConnection).start();
    
    try {
      mFSM.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
    }
    catch (StateMachineException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    
    while (!isStopped) {
      try {
        Message msg = mMessageConnection.readMessage();
        try {
          if (msg.getData() == null) {
            mFSM.process(((Message) msg).getRequest());
          }
          else if (msg.getRequest() == Instruction.PING) {
            LOGGER.info("Got PING request, sending PONG");
            sendMessage(Message.create(Instruction.PONG));
          }
          else {
            LOGGER.info("Got Message: " + msg.getRequest());
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
      mMessageConnection.sendMessage(msg);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
  }
}
