package candis.distributed.test;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;
import candis.example.mini.MiniTask;
import candis.example.mini.MiniControl;
import candis.server.DroidManager;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain{

	private static final Logger LOGGER = Logger.getLogger(TestMain.class.getName());

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {

		DistributedControl t = new MiniControl();

		TestCommunicationIO comio = new TestCommunicationIO<MiniTask>(new MiniTaskFactory(), DroidManager.getInstance());
		comio.initDroids();
		comio.setScheduler(t.initScheduler());
		comio.startScheduler();
		try {

			comio.join();
		}
		catch (InterruptedException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		comio.stopDroids();

	}
}
