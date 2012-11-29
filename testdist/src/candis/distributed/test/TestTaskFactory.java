package candis.distributed.test;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestTaskFactory implements TaskFactory<TestTask> {

	@Override
	public TestTask createTask() {
		return new TestTask();
	}
}
