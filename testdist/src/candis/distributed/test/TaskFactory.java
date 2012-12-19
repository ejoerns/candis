package candis.distributed.test;

import candis.distributed.DistributedRunnable;

/**
 *
 * @author Sebastian Willenborg
 */
public interface TaskFactory<T extends DistributedRunnable> {

	T createTask();
}
