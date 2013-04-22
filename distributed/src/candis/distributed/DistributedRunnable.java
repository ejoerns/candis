package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface DistributedRunnable {

	/**
	 * Sets the initial parameters.
	 *
	 * @param parameter Initial parameter data
	 */
	public void setInitialParameter(DistributedJobParameter parameter);

	/**
	 * Starts the Job with its given parameters.
	 *
	 * @param parameter Parameter for this job
	 * @return Result of the successful done job
	 */
	public DistributedJobResult execute(DistributedJobParameter parameter);

	/**
	 * Requests to stop the current job.
	 */
	public void stopJob();
}
