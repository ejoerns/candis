/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
