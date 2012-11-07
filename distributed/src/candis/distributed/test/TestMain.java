package candis.distributed.test;

import candis.distributed.DistributedControl;
import candis.distributed.Scheduler;
import candis.distributed.SimpleScheduler;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestMain implements DistributedControl{

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here
		DistributedControl t = new TestMain();
		Scheduler s = t.initScheduler();

	}

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
