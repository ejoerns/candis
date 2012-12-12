package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class DistributedTask {

	/**
	 * Sets the initial parameters.
	 *
	 * @param parameter Initial parameter data
	 *
	 */
	abstract public void setInitialParameter(DistributedParameter parameter);

	/**
	 * Starts the Task with its given parameters.
	 *
	 * @param parameter Parameter for this task
	 * @return Result of the successful done task
	 */
	abstract public DistributedResult run(DistributedParameter parameter);

	/**
	 * Requests to stop the current Task.
	 */
	abstract public void stop();
}
