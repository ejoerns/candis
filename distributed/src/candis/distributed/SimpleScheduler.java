package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public class SimpleScheduler implements Scheduler {
	CommunicationIO comIO;
	public SimpleScheduler(DistributedParameter ps[]) {

	}

	@Override
	public void setCommunicationIO(CommunicationIO io) {
		comIO = io;
	}

	@Override
	public void start() {
		// Register Events (new droid), (task done), (task error), (droid lost)
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void abort() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
