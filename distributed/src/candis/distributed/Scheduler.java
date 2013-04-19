package candis.distributed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  protected final Stack<DistributedJobParameter> mParamCache = new Stack<DistributedJobParameter>();
  protected DistributedJobParameter mInitalParameter = null;
  protected JobDistributionIO mJobDistIO;
  /// Results
  private final Map<DistributedJobParameter, DistributedJobResult> mResults = new HashMap<DistributedJobParameter, DistributedJobResult>();
  /// Droids that are currently running
  protected final Map<String, DistributedJobParameter[]> mRunningDroidsList = new HashMap<String, DistributedJobParameter[]>();
  /// Droids that can be scheduled
  protected final Map<String, DroidData> mSchedulabeDroids = new HashMap<String, DroidData>();
  private boolean mEnabled = false;
  private Thread schedulerThread;
  private DistributedControl mControl;
  
  public Scheduler(DistributedControl control) {
    mControl = control;
  }

  /**
   * Sets the JobDistributionIO.
   * Obligatoric function
   *
   * @param io
   */
  public void setJobDistributionIO(JobDistributionIO io) {
    mJobDistIO = io;
  }
  
  public void setInitialParameter(DistributedJobParameter param) {
    mInitalParameter = param;
  }
  
  public DistributedJobParameter getInitialParameter() {
    return mInitalParameter;
  }
  
  public DistributedControl getControl() {
    return mControl;
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

  /**
   * Implement this method to create your own nice and fancy Scheduler.
   *
   * @param droidList List of all schedulable Droids.
   */
  protected abstract void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO);

  /**
   * Used to do inital checks if the droid can be used for scheduling.
   *
   * @return true if accepted, false otherwise
   */
  protected boolean checkAccepted() {
    return true;
  }
  
  public void start() {
    System.out.println("SS: start()");
    if (mEnabled) {
      return;
    }
    mEnabled = true;
    // start scheduler
    schedulerThread = new Thread(new Runnable() {
      public void run() {
        System.out.println("SS: run()");
        while (mEnabled) {
          synchronized (mSchedulabeDroids) {
            if (mControl.hasParametersLeft()) {
              System.out.println("hasParameter -> schedule");
              schedule(mSchedulabeDroids, mJobDistIO);
            }
            try {
              mSchedulabeDroids.wait();
            }
            catch (InterruptedException ex) {
              if (mEnabled) {
                LOGGER.log(Level.SEVERE, null, ex);
              }
            }
          }
        }
      }
    });
    schedulerThread.start();
  }

  /**
   * Called when a new droid connected.
   *
   * @param droidID
   */
  public void registerDroid(String droidID) {
    System.out.println("SS: onNewDroid()");
    // Test if droid is acceptable according to our scheduling rules.
    if (!checkAccepted()) {
      LOGGER.log(Level.INFO, "Rejected Droid {0}", droidID);
      return;
    }
    LOGGER.log(Level.INFO, "Got new Droid {0}", droidID);
    
    synchronized (mSchedulabeDroids) {
      mSchedulabeDroids.put(droidID, mJobDistIO.getDroidData(droidID));
      mSchedulabeDroids.notifyAll();
    }
    //    mJobDistIO.sendBinary(droidID);
  }

  /**
   *
   * @param droidIDs
   */
  public void registerDroids(Set<String> droidIDs) {
    for (String id : droidIDs) {
      registerDroid(id);
    }
  }

  /**
   * Is called when a job is done.
   *
   * @param droidID
   * @param results
   */
  // Job is done and new job can be assigned
  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime) {
    //System.out.println("SS: onJobDone()");
    if (mRunningDroidsList.containsKey(droidID)) {
      DistributedJobParameter[] params = mRunningDroidsList.remove(droidID);
      for (int i = 0; i < results.length; i++) {
        mResults.put(params[i], results[i]);
        releaseResult(params[i], results[i]);
      }
      LOGGER.log(Level.INFO, "Job {0} on {1} done with {2} in {3}ms",
                 new Object[]{jobID, droidID, results, exectime});
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
  public void onDroidError(String droidID, DistributedJobError error) {
    LOGGER.log(Level.SEVERE, "Droid {0}, Error {1}", new Object[]{droidID, error});
    // removed Droids won't stay in the id List
    // therefore it is not neccesary to check, for DROID_LOST in error
    if (mRunningDroidsList.containsKey(droidID)) {
      // put parameters back to cache
      DistributedJobParameter[] params = mRunningDroidsList.get(droidID);
      for (DistributedJobParameter p : params) {
        mParamCache.push(p);
      }
    }
  }
  
  public boolean isDone() {
    // A disabled Scheduler is done! Fact!
    if (!mEnabled) {
      return true;
    }
//    LOGGER.log(Level.FINE,
//               "running {0}, params {1}, available Droids {2}",
//               new Object[]{
//              mRunningDroidsList.size(),
//              mParams.size(),
//              mSchedulabeDroids.size()});
    return ((mRunningDroidsList.size() == 0) && !mControl.hasParametersLeft() && mParamCache.empty());
  }
  
  public void abort() {
    for (String droidID : mSchedulabeDroids.keySet()) {
      if (mRunningDroidsList.containsKey(droidID)) {
        mRunningDroidsList.remove(droidID);
        mJobDistIO.stopJob(droidID);
      }
    }
    mEnabled = false;
    schedulerThread.interrupt();
  }
  
  public Map<DistributedJobParameter, DistributedJobResult> getResults() {
    return mResults;
  }

  /**
   * Pops new parameter from the parameter list.
   *
   * First looks in the internal parameter cache for an available parameter.
   * If no parameter was found, it tries to get new one from task controller.
   *
   * @return Parameter if available, null if no parameter found
   */
  protected DistributedJobParameter getParameter() {
    if (mParamCache.empty()) {
      return mControl.getParameter();
    }
    return mParamCache.pop();
  }

  /**
   * Same as getParameter() but returns array of specified size.
   *
   * @param n Number of parameters to get.
   * @return
   */
  protected DistributedJobParameter[] getParameters(int n) {
    int count = 0;
    ArrayList<DistributedJobParameter> params = new ArrayList<DistributedJobParameter>();
    // first get from internal cache
    while ((mParamCache.size() > 0) && (count < n)) {
      params.add(mParamCache.pop());
      count++;
    }
    n -= count;
    count = 0;
    // try to load remaining parameters from scheduler
    while (count < n) {
      if (!mControl.hasParametersLeft()) {
        break;
      }
      DistributedJobParameter param = mControl.getParameter();
      params.add(param);
      count++;
    }
    
    return params.toArray(new DistributedJobParameter[params.size()]);
  }
}
