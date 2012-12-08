package candis.example.mini;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 *
 * @author Sebastian Willenborg
 */
public class miniControl implements DistributedControl {

	@Override
	public Scheduler initScheduler() {
		Scheduler sch = new SimpleScheduler();
		for (int i = 0; i < 10; i++) {
			sch.addParameter(new MiniParameter(i, 3.5f));
		}
		return sch;
	}

}
