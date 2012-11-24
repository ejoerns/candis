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

	public void onNewDroid(String id);

	public void onTaskDone(String id, DistributedResult result);

	public void onDroidError(String id, DistributedError error);
}
