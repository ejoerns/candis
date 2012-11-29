package candis.distributed;


import java.util.Set;

/**
 *
 * @author Sebastian Willenborg
 */
public interface CommunicationIO {

	int getDroidCount();

	DroidData getDroidData(String droidID);

	void startJob(String droidID, DistributedParameter p);

	void stopJob(String droidID);

	/**
	 * Requests all connected droids.
	 *
	 * @return Set of all currently connected droids
	 */
	Set<String> getConnectedDroids();
}
