package candis.distributed.test;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain implements DistributedControl{
	private static final Logger LOGGER = Logger.getLogger(TestMain.class.getName());

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		DistributedControl t = new TestMain();

		Scheduler s = t.initScheduler();
		TestCommunicationIO comio = new TestCommunicationIO<TestTask>(new TestTaskFactory());
		comio.initDroids();
		s.setCommunicationIO(comio);
		try {
			Thread.sleep(1000);
		}
		catch (InterruptedException ex) {
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
		TestParameter ps[] = new TestParameter[10];
		for(int i=0; i< 10; i++)
		{
			ps[i] = new TestParameter(i);
		}
		Scheduler sch = new SimpleScheduler(ps);
		return sch;
	}
}
