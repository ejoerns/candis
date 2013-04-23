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
            System.out.println("Bling Bling");
            if (mJobDistIO.getControl().hasParametersLeft()) {
              System.out.println("hasParameter -> schedule");
              schedule(mJobDistIO.getAvailableDroids(), mJobDistIO);
            }
            try {
              System.out.println("***** Scheduler waiting ******");
              mSync.wait();
            }
            catch (InterruptedException ex) {
//              if (mEnabled) {
              LOGGER.info("Scheduler thread terminated");
//              }
            }
          }
        }
      }
    });
    mThread.start();
  }

  public void stop() {
    mEnabled = false;
    mThread.interrupt();
  }

  public void setJobDistIO(JobDistributionIO jobDistIO) {
    mJobDistIO = jobDistIO;
  }

  public void doNotify() {
    synchronized (mSync) {
      mSync.notify();
    }
  }
}
