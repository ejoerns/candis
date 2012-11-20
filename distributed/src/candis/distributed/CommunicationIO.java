package candis.distributed;

import candis.distributed.droid.Droid;
import java.util.LinkedList;

/**
 *
 * @author Sebastian Willenborg
 */
public class CommunicationIO {
	private final LinkedList<Droid> knownDroids;

	public CommunicationIO() {
		knownDroids = new LinkedList<Droid>();
	}

	public int getDroidCount() {
		synchronized(knownDroids) {
			return knownDroids.size();
		}
	}

	public Droid getDroid(int i) {
		synchronized(knownDroids) {
			return knownDroids.get(i);
		}
	}

	public void addDroid(Droid droid) {
		synchronized(knownDroids) {
			knownDroids.add(droid);
		}
	}

	public void startTask(Droid droid, DistributedParameter p) {

	}

	public void stopTask(Droid droid)
	{
		
	}
}
