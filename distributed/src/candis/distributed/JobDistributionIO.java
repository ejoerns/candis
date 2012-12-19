package candis.distributed;

import java.util.Set;

/**
 *
 * @author Sebastian Willenborg
 */
public interface JobDistributionIO {

	int getDroidCount();

	DroidData getDroidData(String droidID);

	void sendBinary(String droidID);

	void sendInitialParameter(String droidID, DistributedJobParameter p);

	void startJob(String droidID, DistributedJobParameter p);

	void stopJob(String droidID);

	/**
	 * Requests all connected droids.
	 *
	 * @return Set of all currently connected droids
	 */
	Set<String> getConnectedDroids();

	void join() throws InterruptedException;
}
