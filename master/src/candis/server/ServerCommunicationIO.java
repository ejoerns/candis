package candis.server;


import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.Scheduler;
import candis.distributed.droid.Droid;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO {
	//private final LinkedList<Droid> knownDroids;
	protected Scheduler scheduler;
	protected final DroidManager mDroidManager;

	public ServerCommunicationIO(final DroidManager manager) {
		//knownDroids = new LinkedList<Droid>();
		mDroidManager = manager;
	}

	public void setScheduler(Scheduler s) {
		scheduler = s;
		s.setCommunicationIO(this);
	}

	public void startTask(Droid droid, DistributedParameter p) {
	}

	public void stopTask(Droid droid)
	{

	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	@Override
	public Droid getDroid(int i) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
