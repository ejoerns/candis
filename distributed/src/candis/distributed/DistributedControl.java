package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public interface DistributedControl {

	/**
	 * Requests creation of the Scheduler for it's Project.
	 *
	 * @return Initalized scheduler, ready for distribution
	 */
	public Scheduler initScheduler();

	/**
	 * Gets called when the scheduler finished its work.
	 */
	public void onSchedulerDone();
}