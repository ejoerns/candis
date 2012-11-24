package candis.server;

import candis.distributed.DroidData;
import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.Scheduler;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO {

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

	public void startTask(String id, DistributedParameter p) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void stopTask(String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

//	@Override
//	public Droid getDroid(int i) {
////		throw new UnsupportedOperationException("Not supported yet.");
//		return new Droid(i);// TODO: test
//	}
	@Override
	public DroidData getDroidData(String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

//	@Override
//	public Map<String, Connection> getConnectedDroids() {
//		return mDroidManager.getConnectedDroids();
//	}

}
