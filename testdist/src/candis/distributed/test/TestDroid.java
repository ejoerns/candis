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
import java.io.InterruptedIOException;
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

	private final Droid droid;
	public ObjectInputStream ois;
	public ObjectOutputStream oos;

	private ObjectOutputStream internalOos;
	private ObjectInputStream internalOis;

	private static final Logger LOGGER = Logger.getLogger(TestDroid.class.getName());
	private final DistributedTask task;

	public int getId() {
		return droid.id;
	}

	public Droid getDroid() {
		return droid;
	}

	public TestDroid(int id, DistributedTask task) {
		LOGGER.log(Level.INFO, String.format("New Droid %d", id));
		this.task = task;
		droid = new Droid(id);
		try {
			PipedInputStream incomming = new PipedInputStream();
			PipedInputStream outgoing = new PipedInputStream();
			internalOos = new ObjectOutputStream(new PipedOutputStream(incomming));
			ois = new ObjectInputStream(incomming);
			oos = new ObjectOutputStream(new PipedOutputStream(outgoing));
			internalOis = new ObjectInputStream(outgoing);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

	}

	public void sendMessage(Message msg) {
		try {
			this.oos.writeObject(msg);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void run() {
		LOGGER.log(Level.INFO, String.format("TestDroid %d: start", droid.id));

		try {


				while(true) {
					try {
						Message m_in = (Message) internalOis.readObject();
						if(m_in != null)
						{
							// Handle job
							switch(m_in.getRequest())
							{
								case SEND_JOB:
									DistributedParameter parameters = (DistributedParameter)m_in.getData();
									DistributedResult result = runTask(parameters);
									Message m_result = new Message(Instruction.SEND_RESULT, result);
									internalOos.writeObject(m_result);
									break;
							};


						}
					}
					catch (ClassNotFoundException ex) {
						LOGGER.log(Level.SEVERE, null, ex);
					}
						Thread.sleep(10);
				}


		}
		catch (InterruptedException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %d: interrupted => stop", droid.id));
		}
		catch (InterruptedIOException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %d: interrupted => stop", droid.id));
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		finally {
			LOGGER.log(Level.INFO, String.format("TestDroid %d: stop", droid.id));
		}

	}

	private DistributedResult runTask(DistributedParameter param) {
		return task.run(param);
	}

}
