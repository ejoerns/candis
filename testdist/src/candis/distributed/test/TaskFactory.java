package candis.distributed.test;

import candis.distributed.DistributedTask;

/**
 *
 * @author Sebastian Willenborg
 */
public interface TaskFactory<T extends DistributedTask> {

	T createTask();
}
