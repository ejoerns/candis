package candis.distributed;

/**
 * Binds a scheduler.
 *
 * @author Enrico Joerns
 */
public interface SchedulerBinder {

  /**
   *
   * @param scheduler
   */
  void bindScheduler(Scheduler scheduler);

  /**
   *
   * @param scheduler
   */
  void unbindScheduler(Scheduler scheduler);
}
