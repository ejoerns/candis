/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.distributed.DistributedParameter;
import candis.distributed.DistributedResult;
import candis.distributed.DistributedTask;
import candis.distributed.droid.Droid;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestDroid implements Runnable{

	private Droid droid;
	public ObjectInputStream ois;
	private ObjectOutputStream oos;
	private static final Logger logger = Logger.getLogger(TestDroid.class.getName());
	private final DistributedTask task;

	public int getId() {
		return droid.id;
	}

	public TestDroid(int id, DistributedTask task) {
		Logger.getLogger(TestDroid.class.getName()).log(Level.INFO, String.format("New Droid %d", id));
		this.task = task;
		try {
			droid = new Droid(id);

			PipedInputStream in = new PipedInputStream();

			oos = new ObjectOutputStream(new PipedOutputStream(in));
			ois = new ObjectInputStream(in);

		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}

	}

	@Override
	public void run() {
		logger.log(Level.INFO, String.format("TestDroid %d: start", droid.id));

		try {

				Message m = new Message(Instruction.NO_MSG, null);
				oos.writeObject(m);
				TestParameter p = new TestParameter(4);
				runTask(p);
				//this.notify();
				while(true) {
					Thread.sleep(10);
				}


		}
		catch (InterruptedException iex) {
			logger.log(Level.INFO, String.format("TestDroid %d: interrupted => stop", droid.id));
		}
		catch (IOException ex) {
			logger.log(Level.SEVERE, null, ex);
		}
		finally {
			logger.log(Level.INFO, String.format("TestDroid %d: stop", droid.id));
		}

	}

	private DistributedResult runTask(DistributedParameter param) {
		return task.run(param);

	}

}
