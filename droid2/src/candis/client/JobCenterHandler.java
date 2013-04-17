package candis.client;

import candis.distributed.DistributedJobResult;

/**
 *
 * @author Enrico Joerns
 */
public interface JobCenterHandler {

  public static final int BINARY_RECEIVED = 10;
  public static final int INITIAL_PARAMETER_RECEIVED = 20;
  public static final int JOB_EXECUTION_START = 30;
  public static final int JOB_EXECUTION_DONE = 40;

  /**
   * @note Should be implemented to call on~ handler methods!
   * @param action
   */
  public void onAction(int action, String runnableID);

  public void onBinaryReceived(String runnableID);

  public void onInitialParameterReceived(String runnableID);

  public void onJobExecutionStart(String runnableID, String jobID);

  public void onJobExecutionDone(String runnableID, String jobID, DistributedJobResult[] result, long exectime);

  /**
   * A binary for a job is missing.
   *
   * @param runnableID
   */
  public void onBinaryRequired(String runnableID);
}
