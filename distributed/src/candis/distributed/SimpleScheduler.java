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
 * The number of parameters sent to each droid only depends on its number of
 * cpu cores.
 *
 * @author Sebastian Willenborg
 * @author Enrico Joerns
 */
public class SimpleScheduler extends Scheduler {

  private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class.getName());

  public SimpleScheduler(DistributedControl control) {
    super(control);
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
      DistributedJobParameter[] param = getParameters(data.getProfile().processors);
      mRunningDroidsList.put(id, param); // TODO: place better
      jobDistIO.startJob(id, param);
    }
  }

  @Override
  public void onJobDone(String droidID, String jobID, DistributedJobResult[] results, long exectime) {
    System.out.println("*** SimpleScheduler.onJobDone");
    super.onJobDone(droidID, jobID, results, exectime);
  }
}