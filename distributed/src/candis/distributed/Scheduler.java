package candis.distributed;

import candis.distributed.droid.Droid;

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
	public void onNewDroid(Droid droid);
	public void onTaskDone(Droid droid, DistributedResult result);
	public void onDroidError(Droid droid, DistributedError error);
}
