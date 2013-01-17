package candis.distributed;

import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Simple Scheduler to assign tasks to Droids without special predictions.
 *
 * Every Droid will get DistributedParameters, if a Droid finishes it's task or
 * a new Droid appears it will get its next DistributedParameter
 *
 * @author Sebastian Willenborg
 * @author Enrico Joerns
 */
public class SimpleScheduler extends Scheduler {

  private static final Logger LOGGER = Logger.getLogger(SimpleScheduler.class.getName());

  public void schedule(final Map<String, DroidData> droidList) {
    // do magic stuff here...
    // simply start all available jobs on all available droids
    Iterator<Map.Entry<String, DroidData>> it = droidList.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, DroidData> droid = it.next();
      DroidData data = droid.getValue();
      String id = droid.getKey();
      // TODO: flag setzen
      it.remove();
      DistributedJobParameter param = popParameters();
      mJobDistIO.startJob(id, param);
    }
  }
}