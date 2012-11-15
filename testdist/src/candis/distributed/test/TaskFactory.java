/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.distributed.DistributedTask;

/**
 *
 * @author Sebastian Willenborg
 */
public interface TaskFactory<T extends DistributedTask> {
	T createTask();
}
