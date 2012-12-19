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

	protected void releaseResult(DistributedJobParameter param, DistributedJobResult result) {
		for(ResultReceiver receiver: mReceivers) {
			receiver.onReceiveResult(param, result);
		}
	}

	public abstract void setInitialParameter(DistributedJobParameter param);

	public abstract void addParameter(DistributedJobParameter param);

	public abstract void addParameters(DistributedJobParameter[] params);

	public abstract void setCommunicationIO(JobDistributionIO io);

	public abstract void start();

	public abstract void abort();

	public abstract void onNewDroid(String droidID);

	/**
	 * Is called when a job is done.
	 *
	 * @param droidID
	 * @param result
	 */
	public abstract void onJobDone(String droidID, DistributedJobResult result);

	public abstract void onBinaryRecieved(String droidID);

	public abstract void onInitParameterRecieved(String droidID);

	/**
	 * Is called when an error occurred during computation.
	 *
	 * @param droidID
	 * @param error
	 */
	public abstract void onDroidError(String droidID, DistributedJobError error);

	public abstract boolean isDone();

	public abstract Map<DistributedJobParameter, DistributedJobResult> getResults();
}
