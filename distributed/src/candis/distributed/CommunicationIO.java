package candis.distributed;

import candis.distributed.droid.Droid;

/**
 *
 * @author Sebastian Willenborg
 */
public interface CommunicationIO {
	int getDroidCount();
	Droid getDroid(int i);
	void startTask(Droid d, DistributedParameter p);
	void stopTask(Droid d);

}
