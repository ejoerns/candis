package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface CommunicationIO {
	int getDroidCount();
	DroidData getDroidData(String droidID);
	void startTask(String id, DistributedParameter p);
	void stopTask(String id);

}
