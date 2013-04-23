package candis.distributed;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class WorkerQueue implements Runnable {

  private static final Logger LOGGER = Logger.getLogger(WorkerQueue.class.getName());
  private final List<Runnable> mComIOQueue = new LinkedList<Runnable>();

  @Override
  public void run() {
    Runnable task;
    while (true) {

      synchronized (mComIOQueue) {
        while (mComIOQueue.isEmpty()) {
          try {
            mComIOQueue.wait();
          }
          catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
          }
        }
        task = mComIOQueue.remove(0);
      }

      task.run();
    }
  }

  /**
   * Adds Runnable to Queue.
   *
   * @param task
   */
  public void add(Runnable task) {
    synchronized (mComIOQueue) {
      mComIOQueue.add(task);
      mComIOQueue.notify();
    }
  }
}
