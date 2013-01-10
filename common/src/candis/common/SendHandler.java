package candis.common;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Implements a message queue to send objects over a given ObjectOutputStream.
 *
 * @author Enrico Joerns
 */
public class SendHandler implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(SendHandler.class.getName());
  private final List<Message> mMessageQueue = new LinkedList<Message>();
  private boolean mStop = false; // TODO: use? :)
  private final ObjectOutputStream mObjOutstream;

  public SendHandler(ObjectOutputStream oos) {
    mObjOutstream = oos;
  }

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
              LOGGER.log(Level.SEVERE, null, ex);
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
