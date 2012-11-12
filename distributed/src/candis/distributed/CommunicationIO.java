package candis.distributed;

import candis.distributed.droid.Droid;
import java.util.LinkedList;

/**
 *
 * @author Sebastian Willenborg
 */
public class CommunicationIO {
	private LinkedList<Droid> knownDroids;

	public CommunicationIO() {
		knownDroids = new LinkedList<Droid>();
	}

	public int getDroidCount() {
		return knownDroids.size();
	}

	public void startTask(DistributedParameter p, Droid droid) {

	}
}
