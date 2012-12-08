package candis.distributed.test;

import candis.example.mini.MiniTask;

/**
 *
 * @author Sebastian Willenborg
 */
public class MiniTaskFactory implements TaskFactory<MiniTask> {

	@Override
	public MiniTask createTask() {
		return new MiniTask();
	}
}
