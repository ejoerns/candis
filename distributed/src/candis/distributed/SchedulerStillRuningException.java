package candis.distributed;

/**
 *
 * @author Sebastian Willenborg
 */
public class SchedulerStillRuningException extends Exception{

	public SchedulerStillRuningException(String detailMessage) {
		super(detailMessage);
	}
	
}
