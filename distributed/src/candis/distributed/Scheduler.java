package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface Scheduler {
	public void setCommunicationIO(CommunicationIO io);
	public void start();
	public void abort();
}
