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
	Set<String> getConnectedDroids();
	void onJobDone(final String droidID, final DistributedResult result);

}
