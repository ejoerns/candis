package candis.distributed.test;

import candis.example.mini.MiniRunnable;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniTaskFactory implements TaskFactory<MiniRunnable> {

	@Override
	public MiniRunnable createTask() {
		return new MiniRunnable();
	}
}
