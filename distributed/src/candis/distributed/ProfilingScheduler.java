package candis.distributed;

import java.util.HashMap;
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
public class ProfilingScheduler extends Scheduler implements JobDistributionIO.OnJobDoneListener {

  /// Maximum time a job should be processed [ms]
  private static final long MAX_JOB_PROCESS_TIME = 10000;
  /// Holds profiled droids with corresponding number of parameters to send
  private Map<String, Integer> mProfiledDroids = new HashMap<String, Integer>();

  public ProfilingScheduler() {
    super();
  }

  @Override
  void schedule(String[] droidIDs, JobDistributionIO jobDistIO) {
    for (String droidID : droidIDs) {
      DroidData data = jobDistIO.getDroidData(droidID);
      // send single parameter for profiling
      if (!mProfiledDroids.containsKey(droidID)) {// TODO: send one task per core
        System.out.println("" + data.getProfile().processors + " Profiling parameters sent for " + droidID);
        DistributedJobParameter[] params = jobDistIO.getParameters(data.getProfile().processors);
        jobDistIO.startJob(droidID, params);
      }
      // otherwise start with calculated number of parameters
      else {
        DistributedJobParameter[] params = jobDistIO.getParameters(mProfiledDroids.get(droidID));
        System.out.println("Sending Job with " + params.length + " parameters");
        jobDistIO.startJob(droidID, params);
      }
    }
  }

  @Override
  public void setJobDistIO(JobDistributionIO jobDistIO) {
    super.setJobDistIO(jobDistIO);
    jobDistIO.addJobDoneListener(this);
  }

  public void onJobDone(String droidID, String jobID, String taskID, int results, long exectime) {
    if (!mProfiledDroids.containsKey(droidID)) {
      exectime /= results;// exectime is per job, not per parameter
      if (exectime == 0) {
        exectime = 1;
      }
      System.out.println("*** Exectime: " + exectime);
      int paramcount = (int) (MAX_JOB_PROCESS_TIME / exectime);
      System.out.println("*** Droid should be able to process " + paramcount + " parameters");
      mProfiledDroids.put(droidID, paramcount);
    }
  }
}
