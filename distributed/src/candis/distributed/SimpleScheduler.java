/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed;

/**
 *
 * @author swillenborg
 */
public class SimpleScheduler implements IScheduler {
	CommunicationIO comIO;
	public SimpleScheduler(IDistributedParameter ps[]) {

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
