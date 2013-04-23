package candis.distributed;

import java.util.ArrayList;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class DistributedControl {

  /**
   * Called by Scheduler to give control the chance to setup itself.
   *
   * Might be used to request user input, generate parameter sets, etc.
   */
  public abstract void init();

  /**
   * Gets called when the scheduler finished its work.
   */
  public abstract void onSchedulerDone();

  /**
   * Called by Scheduler to get the initial parameter.
   *
   * @return
   */
  public abstract DistributedJobParameter getInitialParameter();

  /**
   * Called by scheduler to get the smallest available parameter.
   *
   * Provides controller the flexibility to either precalculate parameters
   * or generate parameters at runtime.
   *
   * @return
   */
  public abstract DistributedJobParameter getParameter();

  /**
   * Called by scheduler to get parameter set.
   *
   * @param n
   * @return
   */
  public DistributedJobParameter[] getParameters(int n) {

    int count = 0;
    ArrayList<DistributedJobParameter> params = new ArrayList<DistributedJobParameter>(n);
    while (count < 3) {
      DistributedJobParameter param = getParameter();
      if (param == null) {
        break;
      }
      params.add(param);
      count++;
    }

    return (DistributedJobParameter[]) params.toArray();
  }

  public abstract long getParametersLeft();

  public abstract boolean hasParametersLeft();
}