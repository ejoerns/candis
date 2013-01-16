package candis.distributed;

/**
 * Interface has to be implemented by classes that want to listen to
 * Events from the JobDistributor.
 *
 * @author Enrico Joerns
 */
public interface JobDistributionIOHandler {

  public enum Event {

    SCHEDULER_DONE,}

  public void onEvent(Event event);
}
