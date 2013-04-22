package candis.distributed;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple Scheduler to assign tasks to Droids without special predictions.
 *
 * Every Droid will get DistributedParameters, if a Droid finishes it's task or
 * a new Droid appears it will get its next DistributedParameter(s).
 *
 * If multicore support is enabled, the number of parameters sent to each droid
 * equals its number of cpu cores.
 * Otherwise only one parameter is sent.
 *
 * @author Sebastian Willenborg
 * @author Enrico Joerns
 */
public class SimpleScheduler extends Scheduler {

  private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class.getName());
  private final boolean mMulticore;
  private final int mParamsPerJob;

  public SimpleScheduler(DistributedControl control) {
    this(control, 1, false);
  }

  /**
   *
   * @param control
   * @param parametersPerJob The smalles ammount of parameters that may be sent
   * for each job.
   * Acutal parameters can only be a multiple of this.
   * @param multicore Enables multicore support, i.e. the job size is
   * multiplicated with the number of cores of the target device.
   */
  public SimpleScheduler(DistributedControl control, int parametersPerJob, boolean multicore) {
    super(control);
    mParamsPerJob = parametersPerJob;
    mMulticore = multicore;
  }

  public void schedule(final Map<String, DroidData> droidList, JobDistributionIO jobDistIO) {
    System.out.println("SimpleScheduler.schedule()");
    // do magic stuff here...
    // simply start all available jobs on all available droids
    Iterator<Map.Entry<String, DroidData>> it = droidList.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, DroidData> droid = it.next();
      DroidData data = droid.getValue();
      String id = droid.getKey();
      // TODO: flag setzen
      it.remove();
      // Send parametrs according to cpu cores
      DistributedJobParameter[] params;
      if (mMulticore) {
        params = getParameters(mParamsPerJob * data.getProfile().processors);
      }
      else {
        params = getParameters(mParamsPerJob);
      }
      if (params.length > 0) {
        mRunningDroidsList.put(id, params); // TODO: place better
        jobDistIO.startJob(id, params);
      }
    }
  }

  @Override
  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime) {
    System.out.println("*** SimpleScheduler.onJobDone");
    super.onJobDone(droidID, jobID, results, exectime);
  }
}