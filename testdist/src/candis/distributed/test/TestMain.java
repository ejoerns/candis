package candis.distributed.test;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
import candis.server.DroidManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain implements DistributedControl {

	private static final Logger LOGGER = Logger.getLogger(TestMain.class.getName());

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		//manager.

		DistributedControl t = new TestMain();

		//Scheduler s = t.initScheduler();
		TestCommunicationIO comio = new TestCommunicationIO<TestTask>(new TestTaskFactory(), DroidManager.getInstance());
		comio.initDroids();
		comio.setScheduler(t.initScheduler());

		//s.start();
		try {
			comio.startScheduler();
			for (int i = 0; i < 10; i++) {

				System.out.println(i);
				Thread.sleep(1000);
			}
		} catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		comio.stopDroids();

	}

	/**
	 * Initializes the Scheduler with its Parameters.
	 *
	 * @return The intialized Scheduler
	 */
	@Override
	public Scheduler initScheduler() {

		Scheduler sch = new SimpleScheduler();
		for (int i = 0; i < 10; i++) {
			sch.addParameter(new TestParameter(i));
		}
		return sch;
	}
}
