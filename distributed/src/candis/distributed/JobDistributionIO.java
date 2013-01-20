package candis.distributed;

import java.util.Set;

/**
 * Interface that has to be implemented to provide task distribution for the
 * Scheduler.
 *
 * @author Sebastian Willenborg
 */
public interface JobDistributionIO extends Runnable {

  /**
   * Called by Scheduler to get number of connected droids.
   *
   * @return Number of connected droids
   */
  int getDroidCount();

  /**
   * Called by Scheduler to get the DroidData which might be used for
   * Scheduling decisions.
   *
   * @param droidID
   * @return
   */
  DroidData getDroidData(String droidID);

  /**
   * Called by scheduler to send the task binary.
   *
   * @param droidID ID of Droid to send to
   */
//  void sendBinary(String droidID);

  /**
   * Called by Scheduler to send the initial parameter.
   *
   * @param droidID ID of Droid to send to
   * @param p Initial Parameter to set for the Droid.
   */
//  void sendInitialParameter(String droidID, DistributedJobParameter p);

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

  /**
   * Requests all connected droids.
   *
   * @return Set of all currently connected droids
   */
  Set<String> getConnectedDroids();
}
