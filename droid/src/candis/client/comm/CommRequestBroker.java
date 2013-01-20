package candis.client.comm;

import candis.client.ClientStateMachine;
import candis.common.ClassLoaderWrapper;
import candis.common.Instruction;
import candis.common.Message;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes incoming data from socket.
 *
 * Designed to be tun in a sepearte thread.
 *
 * @author Enrico Joerns
 */
public class CommRequestBroker implements Runnable {

  private static final String TAG = "CRB";
  private static final Logger LOGGER = Logger.getLogger(TAG);
  private boolean isStopped;
  private final FSM mFSM;
  private final ClassLoaderWrapper mClassLoaderWrapper;
  private ObjectInputStream mObjInstream = null;
  private InputStream mInstream;

  public CommRequestBroker(final InputStream instream, final FSM fsm, final ClassLoaderWrapper cl) {
    mInstream = instream;
    mFSM = fsm;
    mClassLoaderWrapper = cl;
    isStopped = false;
  }

  @Override
  public void run() {
    try {
      mObjInstream = new DexloaderObjectInputStream(mInstream);
    }
    catch (StreamCorruptedException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }
    try {
      mFSM.process(ClientStateMachine.ClientTrans.SOCKET_CONNECTED);
    }
    catch (StateMachineException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
    }

    Message m = null;
    while (!isStopped) {
      try {
        Object o = readObject();
        if ((o != null) && (o instanceof Message)) {
          LOGGER.log(Level.INFO, String.format(
                  "Received server Message: %s", ((Message) o).getRequest()));
          try {
            if (((Message) o).getData() == null) {
              mFSM.process(((Message) o).getRequest());
            }
            else {
              mFSM.process(((Message) o).getRequest(), (Object[]) ((Message) o).getData());
            }
          }
          catch (StateMachineException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
          }
        }
        else {
          LOGGER.log(Level.WARNING, "Received data of unknown type!" + o);
          mInstream.close();
          isStopped = true;
        }
      }
      catch (IOException ex) {
        LOGGER.log(Level.ALL, "IOException2");
        LOGGER.log(Level.SEVERE, null, ex);
      }
    }
  }

  public Object readObject() throws IOException {
    Object obj = null;
    Object[] payload = null;
    Message retMsg = null;

    if (mObjInstream == null) {
      LOGGER.warning("InputStream is null");
      return null;
    }
    try {
      int payloadSize = 0;
      System.out.println("DATA?");
      try {
        obj = mObjInstream.readUnshared();
      }
      catch (OptionalDataException odex) {
        LOGGER.info("ODEXODEXODEXODEX");
        System.out.println("avialable: " + mObjInstream.available());
        mObjInstream.close();
        mObjInstream = null;
      }
      System.out.println("DATA!");
      if (obj instanceof Instruction) {
        System.out.println("--> INSTRUCTION: " + (Instruction) obj + " with " + ((Instruction) obj).len + " parameters");
        payloadSize = ((Instruction) obj).len;
        payload = new Object[payloadSize];
        for (int idx = 0; idx < payloadSize; idx++) {
          switch (((Instruction) obj).getType(idx)) {
            case INTEGER:
              System.out.println("--> Reading Int");
              payload[idx] = mObjInstream.readInt();
              break;
            case STRING:
              System.out.println("--> Reading UTF");
              payload[idx] = mObjInstream.readObject();
              break;
            default:
              System.out.println("--> Reading Object");
              payload[idx] = mObjInstream.readUnshared();
              break;
          }
        }
      }
      else {
        System.out.println("UNKNOWN: " + obj);
      }
    }
    catch (ClassNotFoundException ex) {
      LOGGER.log(Level.INFO,
                 String.format("Loaded unknown class: %s", ex.getMessage()));
//      ex.printStackTrace();
    }
    catch (EOFException ex) {
      LOGGER.log(Level.WARNING, "Connection to server was terminated unexpected. (EOFException)");
      LOGGER.log(Level.WARNING, null, ex);
      isStopped = true;
      mObjInstream.close();
      // todo quit loop!
    }
    catch (OptionalDataException ex) {
      System.out.println("OptionalDataException: EOF: " + ex.eof + ", length: " + ex.length);
      LOGGER.log(Level.SEVERE, null, ex);
      mObjInstream.close();
      mObjInstream = null;
    }
    catch (IOException ex) {
      LOGGER.log(Level.SEVERE, null, ex);
      mObjInstream.close();
      mObjInstream = null;
    }

    // TODO: clean solution
    if ((payload == null) || (payload.length == 0)) {
      retMsg = new Message((Instruction) obj);
    }
    else if (payload.length == 1) {
      retMsg = new Message((Instruction) obj, (Serializable) payload[0]);
    }
    else if (payload.length == 2) {
      retMsg = new Message((Instruction) obj, (Serializable) payload[0], (Serializable) payload[1]);
    }
//    System.out.println("Payload 0: " + retMsg.getData(0).getClass());

    return retMsg;
  }
  private ObjectStreamClass mObjectStreamClass;

  /**
   * Resolves class with custom classloader.
   */
  public class DexloaderObjectInputStream extends ObjectInputStream {

    @Override
    public Class resolveClass(ObjectStreamClass desc) throws IOException,
            ClassNotFoundException {

      try {
        return mClassLoaderWrapper.get().loadClass(desc.getName());
      }
      catch (Exception e) {
      }

      return super.resolveClass(desc);
    }

    public DexloaderObjectInputStream(InputStream in) throws IOException {
      super(in);
    }
  }
}
