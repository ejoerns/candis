package candis.client;

import candis.distributed.DistributedJobResult;

/**
 *
 * @author Enrico Joerns
 */
public interface JobCenterHandler {

  /**
   * Invoked if binary was received.
   *
   * @param taskID ID of task binary was received for
   */
  public void onBinaryReceived(String taskID);

  /**
   * Invoked if initial parameter was received.
   *
   * @param taskID ID of task initial parameter was received for
   */
  public void onInitialParameterReceived(String taskID);

  /**
   * Invoked if Job execution started.
   *
   * @param taskID ID of task the job is started for
   * @param jobID ID of started Job
   */
  public void onJobExecutionStart(String taskID, String jobID);

  /**
   * Invoked if Job execution is finished.
   *
   * @param taskID ID of task
   * @param jobID ID of finished job
   * @param result Job result
   * @param exectime Execution duration
   */
  public void onJobExecutionDone(String taskID, String jobID, DistributedJobResult[] result, long exectime);

  /**
   * Invoked if a binary for a Job is missing.
   *
   * @param taskID ID of task binary is required for
   */
  public void onBinaryRequired(String taskID);

  /**
   * Invoked if a Job was rejected for any reason.
   *
   * @param taskID ID of rejected Job
   */
  public void onJobRejected(String taskID, String jobID);
}
