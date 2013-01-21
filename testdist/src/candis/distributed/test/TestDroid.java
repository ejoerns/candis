package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import candis.server.CDBLoader;
import candis.server.JobDistributionIOServer;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestDroid extends DroidData implements Runnable {

	private final String mID;
	private ObjectInputStream ois;
	private ObjectOutputStream oos;
	private ObjectOutputStream internalOos;
	private ObjectInputStream internalOis;
	private static final Logger LOGGER = Logger.getLogger(TestDroid.class.getName());
	private final DistributedRunnable task;

	public String getId() {
		return mID;
	}

	public TestDroid(int id, JobDistributionIOServer jobIOServer, String jobID) {
		super(false, new StaticProfile());
		LOGGER.log(Level.INFO, String.format("New Droid %d", id));

		this.task = jobIOServer.getCDBLoader().getDistributedRunnable(jobID);
		mID = Integer.toString(id);
		try {
			// Direction: Droid is reading
			InOutStreams incomming = new InOutStreams(jobIOServer.getCDBLoader());
			internalOos = incomming.getOutputStream();
			ois = incomming.getInputStream();

			// Direction: Droid is writing
			InOutStreams outgoing = new InOutStreams(jobIOServer.getCDBLoader());
			oos = outgoing.getOutputStream();
			internalOis = outgoing.getInputStream();
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

	}

	@Override
	public void run() {
		LOGGER.log(Level.FINE, String.format("TestDroid %s: start", mID));

		try {

			while (true) {
				try {

					LOGGER.log(Level.INFO, "Waiting for a new Message");
					Object msg = internalOis.readObject();
					if (msg instanceof Message) {
						Message m_in = (Message) msg;

						if (m_in != null) {
							LOGGER.log(Level.INFO, "Droid received message: {0}", m_in.getRequest());

							// Handle job
							switch (m_in.getRequest()) {
								case SEND_BINARY:
									internalOos.writeObject(new Message(Instruction.ACK, (Serializable) null));
									break;
								case SEND_INITIAL:
									DistributedJobParameter initial = (DistributedJobParameter) m_in.getData(0);
									task.setInitialParameter(initial);
									internalOos.writeObject(new Message(Instruction.ACK, (Serializable) null));
									break;
								case SEND_JOB:
									internalOos.writeObject(new Message(Instruction.ACK, (Serializable) null));
									DistributedJobParameter parameters = (DistributedJobParameter) m_in.getData(0);
									DistributedJobResult result = runTask(parameters);
									Message m_result = new Message(Instruction.SEND_RESULT, result);
									internalOos.writeObject(m_result);
									break;
							}
						}
					}
				}
				catch (ClassNotFoundException ex) {
					LOGGER.log(Level.SEVERE, null, ex);
				}
			}


		}
		catch (InterruptedIOException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: interrupted => stop", mID), iex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		finally {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: stop", mID));
		}


	}

	private DistributedJobResult runTask(DistributedJobParameter param) {
		return task.runJob(param);
	}

	public ObjectInputStream getInputStream() {
		return ois;
	}

	public ObjectOutputStream getOutputStream() {
		return oos;
	}
}
