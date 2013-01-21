package candis.distributed.test;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import candis.server.JobDistributionIOServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Sebastian Willenborg
 */
public class TestDroid extends DroidData implements Runnable {

	private final String mID;
	private InputStream ois;
	private OutputStream oos;
	private OutputStream internalOos;
	private InputStream internalOis;
	private MessageConnection messageConnection;
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
			InOutStreams incomming = new InOutStreams(/*jobIOServer.getCDBLoader()*/);
			internalOos = incomming.getOutputStream();
			ois = incomming.getInputStream();

			// Direction: Droid is writing
			InOutStreams outgoing = new InOutStreams(/*jobIOServer.getCDBLoader()*/);
			oos = outgoing.getOutputStream();
			internalOis = outgoing.getInputStream();
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		messageConnection = new MessageConnection(internalOis, internalOos, jobIOServer.getCDBLoader().getClassLoaderWrapper());

	}

	@Override
	public void run() {
		LOGGER.log(Level.FINE, String.format("TestDroid %s: start", mID));
		DistributedJobParameter jobParameter = null;
		boolean gotInit = false;
		try {

			while (true) {


				LOGGER.log(Level.INFO, "Waiting for a new Message");

				Message m_in = messageConnection.readMessage();


				LOGGER.log(Level.INFO, "Droid received message: {0}", m_in.getRequest());

				// Handle job
				switch (m_in.getRequest()) {
					case SEND_BINARY:
						messageConnection.sendMessage(new Message(Instruction.ACK, (Serializable) null));
						break;
					case SEND_INITIAL:
						DistributedJobParameter initial = (DistributedJobParameter) m_in.getData(1);
						task.setInitialParameter(initial);
						messageConnection.sendMessage(new Message(Instruction.ACK, (Serializable) null));
						gotInit = true;
						if (jobParameter != null) {

							DistributedJobResult result = runTask(jobParameter);
							Message m_result = new Message(Instruction.SEND_RESULT, m_in.getData(0), result);
							messageConnection.sendMessage(m_result);
							jobParameter = null;
						}
						break;
					case SEND_JOB:
						if (!gotInit) {
							messageConnection.sendMessage(new Message(Instruction.REQUEST_INITIAL, m_in.getData(0)));
							jobParameter = (DistributedJobParameter) m_in.getData(1);
							break;
						}
						jobParameter = null;
						messageConnection.sendMessage(new Message(Instruction.ACK, (Serializable) null));
						DistributedJobParameter parameters = (DistributedJobParameter) m_in.getData(1);
						DistributedJobResult result = runTask(parameters);
						Message m_result = new Message(Instruction.SEND_RESULT, m_in.getData(0), result);
						messageConnection.sendMessage(m_result);
						break;
				}


			}

		}
		catch (InterruptedIOException iex) {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: interrupted => stop", mID));
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

	public InputStream getInputStream() {
		return ois;
	}

	public OutputStream getOutputStream() {
		return oos;
	}
}
