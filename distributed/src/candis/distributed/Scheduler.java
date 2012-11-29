package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface Scheduler {

	public void addParameter(DistributedParameter param);

	public void addParameters(DistributedParameter[] params);

	public void setCommunicationIO(CommunicationIO io);

	public void start();

	public void abort();

	public void onNewDroid(String droidID);

	/**
	 * Is called when a job is done.
	 *
	 * @param droidID
	 * @param result
	 */
	public void onJobDone(String droidID, DistributedResult result);

	/**
	 * Is called when an error occurred during computation.
	 *
	 * @param droidID
	 * @param error
	 */
	public void onDroidError(String droidID, DistributedError error);

	public boolean isDone();
}
