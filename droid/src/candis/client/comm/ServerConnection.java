package candis.client.comm;

import android.app.FragmentManager;
import android.content.Context;
import candis.client.ClientStateMachine;
import candis.client.DroidContext;
import candis.client.JobCenter;
import candis.common.ClassLoaderWrapper;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.common.SendHandler;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.io.StreamCorruptedException;
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
  private final ClassLoaderWrapper mClassLoaderWrapper;
  private SendHandler mSendHandler;
  private final MessageConnection mMessageConnection;

  public ServerConnection(Socket socket,
                          final ClassLoaderWrapper cl,
                          final DroidContext dcontext,
                          final Context context,
                          final FragmentManager fragmanager,
                          final JobCenter jobcenter) {
    mClassLoaderWrapper = cl;
    mMessageConnection = new MessageConnection(socket, mClassLoaderWrapper);
    mFSM = new ClientStateMachine(this, dcontext, context, fragmanager, jobcenter);
    isStopped = false;
  }

  // TODO: better hack?
  public MessageConnection getMessageConnection() {
    return mMessageConnection;
  }

  /**
   * Thread to handle incoming requests.
   */
  @Override
  public void run() {
    mSendHandler = new SendHandler(mMessageConnection);
    new Thread(mSendHandler).start();
//    try {
//      mObjInstream = new DexloaderObjectInputStream(mInstream);
//    }
//    catch (StreamCorruptedException ex) {
//      LOGGER.log(Level.SEVERE, null, ex);
//    }
//    catch (IOException ex) {
//      LOGGER.log(Level.SEVERE, null, ex);
//    }
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
          else {
            mFSM.process(((Message) msg).getRequest(), (Object[]) ((Message) msg).getData());
          }
        }
        catch (StateMachineException ex) {
          Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
//        }
//        else {
//          LOGGER.log(Level.WARNING, "Received data of unknown type!" + msg);
//          mInstream.close();
//          isStopped = true;
//        }
      }
      catch (IOException ex) {
        LOGGER.log(Level.ALL, "IOException2");
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
    mSendHandler.addToQueue(msg);
  }
}
