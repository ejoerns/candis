package candis.server;

import candis.distributed.CommunicationIO;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DroidData;
import candis.distributed.Scheduler;
import java.io.IOException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class ServerCommunicationIO implements CommunicationIO, Runnable {

	protected Scheduler scheduler;
	protected final DroidManager mDroidManager;

	public ServerCommunicationIO(final DroidManager manager) {
		mDroidManager = manager;
	}

	public void setScheduler(Scheduler s) {
		scheduler = s;
		s.setCommunicationIO(this);
	}

	protected Connection getDroidConnection(String droidID) {
		return mDroidManager.getConnectedDroids().get(droidID);
	}

	@Override
	public void startJob(String id, DistributedParameter p) {
		System.out.println("startTask()");
		Connection d = getDroidConnection(id);
		try {
			d.sendJob(p);
		} catch (IOException ex) {
			Logger.getLogger(ServerCommunicationIO.class.getName()).log(Level.SEVERE, null, ex);
		}

	}

	@Override
	public void stopJob(final String id) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getDroidCount() {
		return mDroidManager.getConnectedDroids().size();
	}

	@Override
	public DroidData getDroidData(final String droidID) {
		return mDroidManager.getKnownDroids().get(droidID);
	}

	@Override
	public void run() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Set<String> getConnectedDroids() {
		return mDroidManager.getConnectedDroids().keySet();
	}

	public void startScheduler() {
		scheduler.start();
	}

	@Override
	public void onJobDone(final String droidID, final DistributedResult result) {
		scheduler.onJobDone(droidID, result);
	}

}
