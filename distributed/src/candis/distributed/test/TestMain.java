/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.distributed.IDistributedControl;
import candis.distributed.IScheduler;
import candis.distributed.SimpleScheduler;

/**
 *
 * @author swillenborg
 */
public class TestMain implements IDistributedControl{

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		// TODO code application logic here
		TestMain t = new TestMain();
		IScheduler s = t.initScheduler();

	}

	@Override
	public IScheduler initScheduler() {
		TestParameter ps[] = new TestParameter[10];
		for(int i=0; i< 10; i++)
		{
			ps[i] = new TestParameter(i);
		}
		IScheduler sch = new SimpleScheduler(ps);
		return sch;
	}
}
