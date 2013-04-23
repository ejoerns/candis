package candis.distributed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Profiling Scheduler.
 *
 * Scheduling Strategy:
 * Send small Jobs to new clients to determine their performance.
 *
 * Detailed:
 * MAX_JOB_PROCESS_TIME specifies the maximum time we allow a job to run.
 * Based on this and on the execution time received the number of parameters
 * for a job is calculated.
 * Further jobs for the respective client are sent with this ammount of
 * parameters.
 *
 * @author Enrico Joerns
 */
public class ProfilingScheduler extends OldScheduler {

  /// Maximum time a job should be processed [ms]
  private static final long MAX_JOB_PROCESS_TIME = 10000;
  /// Holds profiled droids with corresponding number of parameters to send
  private Map<String, Integer> mProfiledDroids = new HashMap<String, Integer>();

  public ProfilingScheduler(DistributedControl control) {
    super(control);
  }

  @Override
  protected void schedule(Map<String, DroidData> droidList, JobDistributionIO jobDistIO) {
    Iterator<Map.Entry<String, DroidData>> it = droidList.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, DroidData> droid = it.next();
      DroidData data = droid.getValue();
      String droidID = droid.getKey();
      // TODO: flag setzen
      it.remove();
      // send single parameter for profiling
      if (!mProfiledDroids.containsKey(droidID)) {// TODO: send one task per core
        System.out.println("" + data.getProfile().processors + " Profiling parameters sent for " + droidID);
        DistributedJobParameter[] params = getParameters(data.getProfile().processors);
        mRunningDroidsList.put(droidID, params); // TODO: place better
        jobDistIO.startJob(droidID, params);
      }
      // otherwise start with calculated number of parameters
      else {
        DistributedJobParameter[] params = getParameters(mProfiledDroids.get(droidID));
        mRunningDroidsList.put(droidID, params);
        System.out.println("Sending Job with " + params.length + " parameters");
        jobDistIO.startJob(droidID, params);
      }
    }
  }

  @Override
  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime) {
    if (!mProfiledDroids.containsKey(droidID)) {
      exectime /= results.length;// exectime is per job, not per parameter
      if (exectime == 0) {
        exectime = 1;
      }
      System.out.println("*** Exectime: " + exectime);
      int paramcount = (int) (MAX_JOB_PROCESS_TIME / exectime);
      System.out.println("*** Droid should be able to process " + paramcount + " parameters");
      mProfiledDroids.put(droidID, paramcount);
    }
    super.onJobDone(droidID, jobID, results, exectime);
  }
}
