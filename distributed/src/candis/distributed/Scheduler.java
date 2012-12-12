package candis.distributed;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Sebastian Willenborg
 */
public abstract class Scheduler {
	protected final List<ResultReceiver> mReceivers = new LinkedList<ResultReceiver>();

	public void addResultReceiver(ResultReceiver receiver) {
		mReceivers.add(receiver);
	}

	protected void releaseResult(DistributedParameter param, DistributedResult result) {
		for(ResultReceiver receiver: mReceivers) {
			receiver.onReceiveResult(param, result);
		}
	}

	public abstract void setInitialParameter(DistributedParameter param);

	public abstract void addParameter(DistributedParameter param);

	public abstract void addParameters(DistributedParameter[] params);

	public abstract void setCommunicationIO(CommunicationIO io);

	public abstract void start();

	public abstract void abort();

	public abstract void onNewDroid(String droidID);

	/**
	 * Is called when a job is done.
	 *
	 * @param droidID
	 * @param result
	 */
	public abstract void onJobDone(String droidID, DistributedResult result);

	public abstract void onBinaryRecieved(String droidID);

	public abstract void onInitParameterRecieved(String droidID);

	/**
	 * Is called when an error occurred during computation.
	 *
	 * @param droidID
	 * @param error
	 */
	public abstract void onDroidError(String droidID, DistributedError error);

	public abstract boolean isDone();

	public abstract Map<DistributedParameter, DistributedResult> getResults();
}
