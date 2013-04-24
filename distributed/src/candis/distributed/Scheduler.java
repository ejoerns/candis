package candis.distributed;

import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public abstract class Scheduler {

  private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
  private Thread mThread;
  private JobDistributionIO mJobDistIO;
  private boolean mEnabled;
  private DistributedControl mControl;
  private final Object mSync = new Object();

  /**
   * Implement this method to create your own nice and fancy Scheduler.
   *
   * @param droidIDs List of all schedulable Droids.
   */
  abstract void schedule(String[] droidIDs, JobDistributionIO jobDistIO);

  public void start(final String taskID) {
    mThread = new Thread(new Runnable() {
      public void run() {
        LOGGER.info(String.format("Scheduler for task %s started.", taskID));
        mJobDistIO.setControl(taskID);
        mJobDistIO.getControl().init();// TODO: place here?

        mEnabled = true;
        while (mEnabled) {
          synchronized (mSync) {
            schedule(mJobDistIO.getAvailableDroids(), mJobDistIO);
            try {
              mSync.wait();
            }
            catch (InterruptedException ex) {
              LOGGER.info("Scheduler thread terminated");
              mEnabled = false;
            }
          }
        }
      }
    });
    mThread.start();
  }

  public void stop() {
    mThread.interrupt();
  }

  public void setJobDistIO(JobDistributionIO jobDistIO) {
    mJobDistIO = jobDistIO;
  }

  public JobDistributionIO getJobDistIO() {
    return mJobDistIO;
  }

  public void doNotify() {
    synchronized (mSync) {
      mSync.notify();
    }
  }
}
