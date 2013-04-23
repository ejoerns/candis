package candis.distributed;

/**
 * Interface that has to be implemented to provide task distribution for the
 * Scheduler.
 *
 * @author Sebastian Willenborg
 */
public interface JobDistributionIO {

  /**
   * Called by Scheduler to get the DroidData which might be used for
   * Scheduling decisions.
   *
   * @param droidID
   * @return
   */
  DroidData getDroidData(String droidID);

  /**
   *
   * Called by Scheduler to get the tasks controller.
   *
   * @param droidID
   * @return
   */
  void setControl(String droidID);

  /**
   *
   * @return
   */
  DistributedControl getControl();

  /**
   * Called by Scheduler to send an initial parameter.
   *
   * @param droidID ID of Droid to send to
   * @param params Initial Parameter to set for the Droid.
   */
  void sendInitialParameter(String droidID, DistributedJobParameter params);

  /**
   * Called by Scheduler to start a Job.
   *
   * @param droidID ID of droid to start
   * @param params Parameter to start the Job with
   */
  void startJob(String droidID, DistributedJobParameter[] params);

  /**
   * Called by Scheduler to stop a Job.
   *
   * @param droidID ID of droid to stop
   */
  void stopJob(String droidID);

  /**
   * Called by Scheduler to fetch new parameters.
   *
   * @param n Number of parameters to get
   * @return
   */
  DistributedJobParameter[] getParameters(int n);

  /**
   * Called by Scheduler to get available droids that might be scheduled.
   *
   * @return List of available droids
   */
  String[] getAvailableDroids();

  /**
   * Sets timeout for receiving ACK.
   *
   * @param millis
   */
  void setAckTimeout(long millis);

  /**
   * Sets timeout for receiving results.
   *
   * @param millis
   */
  void setJobTimeout(long millis);
}
