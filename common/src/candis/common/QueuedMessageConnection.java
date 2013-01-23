package candis.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Messages to send are pushed to a message queue that can be sent by seperate
 * Thread.
 *
 * The QueuedMessageConnection must be started by running it in a Thread:
 * <code>
 * new Thread(new QueuedMessageConnection(...)).start;
 * </code>
 *
 * @author Enrico Joerns
 */
public class QueuedMessageConnection extends MessageConnection implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(QueuedMessageConnection.class.getName());
  private final List<Message> mMessageQueue = new LinkedList<Message>();
  private boolean mStop = false; // TODO: use? :)

  public QueuedMessageConnection(Socket socket, ClassLoaderWrapper clw) throws IOException {
    super(socket, clw);
  }

  public QueuedMessageConnection(InputStream in, OutputStream out, ClassLoaderWrapper clw) {
    super(in, out, clw);
  }

  @Override
  public void sendMessage(final Message msg) throws IOException {
    synchronized (mMessageQueue) {
      mMessageQueue.add(msg);
      mMessageQueue.notify();
    }
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
              super.sendMessage(mMessageQueue.remove(0));
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
}
