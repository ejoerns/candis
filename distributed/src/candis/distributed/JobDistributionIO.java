package candis.distributed;

/**
 * Interface that has to be implemented to provide task distribution for the
 * Scheduler.
 *
 * @author Sebastian Willenborg
 */
public interface JobDistributionIO extends Runnable {

  /**
   * Called by Scheduler to get the DroidData which might be used for
   * Scheduling decisions.
   *
   * @param droidID
   * @return
   */
  DroidData getDroidData(String droidID);

  /**
   * Called by Scheduler to send an initial parameter.
   *
   * @param droidID ID of Droid to send to
   * @param p Initial Parameter to set for the Droid.
   */
  void sendInitialParameter(String droidID, DistributedJobParameter p);

  /**
   * Called by Scheduler to start a Job.
   *
   * @param droidID ID of droid to start
   * @param p Parameter to start the Job with
   */
  void startJob(String droidID, DistributedJobParameter p);

  /**
   * Called by Scheduler to stop a Job.
   *
   * @param droidID ID of droid to stop
   */
  void stopJob(String droidID);
}
