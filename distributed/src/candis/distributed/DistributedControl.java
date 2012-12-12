package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface DistributedControl {

	public Scheduler initScheduler();
	public void schedulerDone();
}
