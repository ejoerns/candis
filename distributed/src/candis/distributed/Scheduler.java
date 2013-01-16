package candis.distributed;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class Scheduler {

  private static final Logger LOGGER = Logger.getLogger(Scheduler.class.getName());
  // List of callback receivers
  protected final List<ResultReceiver> mReceivers = new LinkedList<ResultReceiver>();
  protected final Stack<DistributedJobParameter> mParams = new Stack<DistributedJobParameter>();
  protected DistributedJobParameter mInitalParameter = null;
  protected JobDistributionIO mJobDistIO;
  /// Results
  private final Map<DistributedJobParameter, DistributedJobResult> mResults = new HashMap<DistributedJobParameter, DistributedJobResult>();
  /// Droids that are currently running
  protected final Map<String, DistributedJobParameter> mRunningDroidsList = new HashMap<String, DistributedJobParameter>();
  /// Droids that can be scheduled
  protected final Map<String, DroidData> mSchedulabeDroids = new HashMap<String, DroidData>();
  private boolean mEnabled = false;

  /**
   * Sets the JobDistributionIO.
   * Obligatoric function
   *
   * @param io
   */
  public void setCommunicationIO(JobDistributionIO io) {
    mJobDistIO = io;
  }

  /**
   * Adds a listener for Results.
   *
   * @param receiver
   */
  public void addResultReceiver(ResultReceiver receiver) {
    mReceivers.add(receiver);
  }

  /**
   * Called by Scheduler to publish results.
   *
   * @param param
   * @param result
   */
  protected void releaseResult(DistributedJobParameter param, DistributedJobResult result) {
    for (ResultReceiver receiver : mReceivers) {
      receiver.onReceiveResult(param, result);
    }
  }

  public void setInitialParameter(DistributedJobParameter param) {
    mInitalParameter = param;
  }

  public void addParameter(DistributedJobParameter param) {
    mParams.push(param);
  }

  public void addParameters(DistributedJobParameter[] params) {
    for (DistributedJobParameter p : params) {
      this.mParams.push(p);
    }
  }

  /**
   * Implement this method to create your own nice and fancy Scheduler.
   *
   * @param droidList List of all schedulable Droids.
   */
  protected abstract void schedule(Map<String, DroidData> droidList);

  public void start() {
    System.out.println("SS: start()");
    if (mEnabled) {
      return;
    }
    mEnabled = true;
    // start scheduler
    new Thread(new Runnable() {
      public void run() {
        System.out.println("SS: XXXXXXXXXXXXXXXXXXXXXXXXX new thread");
        while (mEnabled) {
          synchronized (mSchedulabeDroids) {
            if (hasParameter()) {
              schedule(mSchedulabeDroids);
            }
            try {
              // todo: seb mach ma!!!!
              // Weiß er was er hier machen würde?
              mSchedulabeDroids.wait();
            }
            catch (InterruptedException ex) {
              LOGGER.log(Level.SEVERE, null, ex);
            }
          }
        }
      }
    }).start();
  }

  /**
   * Used to do inital checks if the droid can be used for scheduling.
   *
   * @return true if accepted, false otherwise
   */
  protected boolean checkAccepted() {
    return true;
  }

  /**
   * Called when a new droid connected.
   *
   * @param droidID
   */
  public void onNewDroid(String droidID) {
    System.out.println("SS: onNewDroid()");
    // Test if droid is acceptable according to our scheduling rules.
    if (!checkAccepted()) {
      LOGGER.log(Level.INFO, "Rejected Droid {0}", droidID);
      return;
    }
    LOGGER.log(Level.INFO, "Got new Droid {0}", droidID);
    // Send Binary
    mJobDistIO.sendBinary(droidID);
  }

  /**
   * Called when droid received binary.
   *
   * @param droidID
   */
  public void onBinaryRecieved(String droidID) {
    System.out.println("SS: onBinaryRecieved()");
    // Send inital Parameter
    mJobDistIO.sendInitialParameter(droidID, mInitalParameter);
  }

  public void onInitParameterRecieved(String droidID) {
    System.out.println("SS: onInitParameterRecieved()");
    // add to list of schedulable tasks
    synchronized (mSchedulabeDroids) {
      mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
      mSchedulabeDroids.notifyAll();
    }
  }

  /**
   * Is called when a job is done.
   *
   * @param droidID
   * @param result
   */
  // Job is done and new job can be assigned
  public void onJobDone(String droidID, DistributedJobResult result) {
    System.out.println("SS: onJobDone()");
    if (mRunningDroidsList.containsKey(droidID)) {
      DistributedJobParameter p = mRunningDroidsList.remove(droidID);
      mResults.put(p, result);
      releaseResult(p, result);
      LOGGER.log(Level.INFO, "Param {0} on {1} done with {2}", new Object[]{p, droidID, result});
    }
    synchronized (mSchedulabeDroids) {
      mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
      mSchedulabeDroids.notifyAll();
    }
  }

  /**
   * Is called when an error occurred during computation.
   *
   * @param droidID
   * @param error
   */
  // ups..
  public void onDroidError(String id, DistributedJobError error) {
    LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[]{id, error});
    // removed Droids won't stay in the id List
    // therefore it is not neccesary to check, for DROID_LOST in error
    if (mRunningDroidsList.containsKey(id)) {
      DistributedJobParameter p = mRunningDroidsList.get(id);
      addParameter(p);
    }
  }

  public boolean isDone() {
//    LOGGER.log(Level.FINE,
//               "running {0}, params {1}, available Droids {2}",
//               new Object[]{
//              mRunningDroidsList.size(),
//              mParams.size(),
//              mJobDistIO.getDroidCount()});
//    return (mRunningDroidsList.size() + mParams.size()) == 0;
    return false;
  }

  public void abort() {
    for (String droidID : mSchedulabeDroids.keySet()) {
      if (mRunningDroidsList.containsKey(droidID)) {
        mRunningDroidsList.remove(droidID);
        mJobDistIO.stopJob(droidID);
      }
    }
    mEnabled = false;
  }

  public Map<DistributedJobParameter, DistributedJobResult> getResults() {
    return mResults;
  }

  /**
   * Tests if parameter stack is empty
   *
   * @return
   */
  protected boolean hasParameter() {
    return !mParams.empty();
  }

  /**
   * Pops new parameter from the parameter list
   *
   * @return Parameter if available, null if no parameter found
   */
  protected DistributedJobParameter popParameters() {
    if (mParams.empty()) {
      return null;
    }
    return mParams.pop();
  }

  /**
   * Entry for process table.
   */
  protected class ScheduleDroid {

    public ScheduleDroid(String id, DroidData data) {
      this.id = id;
      this.running = false;
      this.currentParameter = null;
      this.data = data;
    }
    /// id of the Droid
    public String id;
    /// true if Droid is running a job
    public boolean running;
    /// if Droid is runnning: currently assigned task
    public DistributedJobParameter currentParameter;
    /// DroidData of Droid
    public DroidData data;
  }
}
