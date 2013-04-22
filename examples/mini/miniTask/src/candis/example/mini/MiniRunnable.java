package candis.example.mini;

import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;

/**
 * Example Task.
 * Multiplies MiniParameter.foo with MiniParamter.bar
 */
public class MiniRunnable implements DistributedRunnable {

  private MiniInitParameter initial;

  /**
   * Gets called when the Task should be aborted.
   */
  @Override
  public void stopJob() {
    // Nothing to do here
  }

  /**
   * Gets called to start the Task with given parameter.
   * Contains main code for this Task.
   *
   * @param parameter Parameter (MiniParameter) specifying the current Task
   * @return The generated MiniResult, when the task is finished
   */
  @Override
  public DistributedJobResult runJob(DistributedJobParameter parameter) {
    // Cast incomming Parameter
    MiniJobParameter p = (MiniJobParameter) parameter;
    System.out.println(String.format("HEY, I AM THE RUNNABLE, MY PARAMETERS ARE: %s and %s", p.bar, p.foo));
    return new MiniJobResult(p.foo * p.bar + initial.offset);
  }

  /**
   * Gets called to set the initial parameter.
   *
   * @param parameter Transfered initial parameter
   */
  @Override
  public void setInitialParameter(DistributedJobParameter parameter) {
    initial = (MiniInitParameter) parameter;
  }
}