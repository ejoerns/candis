package candis.distributed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Enrico Joerns
 */
public class ProfilingScheduler extends Scheduler {

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
      if (!mProfiledDroids.containsKey(droidID)) {
        System.out.println("Profiling parameter sent for " + droidID);
        DistributedJobParameter param = getParameter();
        mRunningDroidsList.put(droidID, new DistributedJobParameter[]{param}); // TODO: place better
        jobDistIO.startJob(droidID, new DistributedJobParameter[]{param});
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
    System.out.println("onJobDone()");
    if (!mProfiledDroids.containsKey(droidID)) {
      int paramcount = (int) (MAX_JOB_PROCESS_TIME / exectime);
      System.out.println("*** Droid should be able to process " + paramcount + " parameters");
      mProfiledDroids.put(droidID, paramcount);
    }
    super.onJobDone(droidID, jobID, results, exectime);
  }
}
