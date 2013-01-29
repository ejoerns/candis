package candis.distributed.test;

import candis.common.ClassloaderObjectInputStream;
import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.distributed.DistributedJobParameter;
import candis.distributed.DistributedJobResult;
import candis.distributed.DistributedRunnable;
import candis.distributed.DroidData;
import candis.distributed.droid.StaticProfile;
import candis.server.JobDistributionIOServer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
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
	private final ClassLoader mClassLoader;
	private Thread mJobThread;

	public String getId() {
		return mID;
	}

	public TestDroid(int id, JobDistributionIOServer jobIOServer, String jobID) {
		super(false, new StaticProfile());
		LOGGER.log(Level.INFO, String.format("New Droid %d", id));

		this.task = jobIOServer.getCDBLoader().getDistributedRunnable(jobID);
		mClassLoader = jobIOServer.getCDBLoader().getClassLoader(jobID);
		mID = Integer.toString(id);
		try {
			// Direction: Droid is reading
			InOutStreams incomming = new InOutStreams();
			internalOos = incomming.getOutputStream();
			ois = incomming.getInputStream();

			// Direction: Droid is writing
			InOutStreams outgoing = new InOutStreams();
			oos = outgoing.getOutputStream();
			internalOis = outgoing.getInputStream();
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}

		messageConnection = new MessageConnection(internalOis, internalOos);//, jobIOServer.getCDBLoader().getClassLoaderWrapper());

	}

	public static byte[] serializeJobResult(DistributedJobResult jobResult) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(jobResult);
			oos.close();
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return baos.toByteArray();
	}

	public static DistributedJobParameter deserializeJobParameter(byte[] rawdata, ClassLoader classLoader) {
		ObjectInputStream objInstream;
		Object obj = null;
		try {
			objInstream = new ClassloaderObjectInputStream(
							new ByteArrayInputStream(rawdata),
							classLoader);
			obj = objInstream.readObject();
			objInstream.close();
		}
		catch (OptionalDataException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (ClassNotFoundException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		return (DistributedJobParameter) obj;
	}

	@Override
	public void run() {
		LOGGER.log(Level.FINE, String.format("TestDroid %s: start", mID));
		byte[] jobBytes = null;
		boolean gotInit = false;
		Serializable jobid;
		try {

			while (true) {

				LOGGER.log(Level.FINE, "Waiting for a new Message");

				Message m_in = messageConnection.readMessage();

				LOGGER.log(Level.FINE, "Droid received message: {0}", m_in.getRequest());

				// Handle job
				switch (m_in.getRequest()) {
					case SEND_BINARY:
						messageConnection.sendMessage(new Message(Instruction.ACK, (Serializable) null));
						break;
					case SEND_INITIAL:
						//JobBytes = m_in.getData(1);
						DistributedJobParameter initial = deserializeJobParameter((byte[]) m_in.getData(1), mClassLoader);
						task.setInitialParameter(initial);
						jobid = m_in.getData(0);
						gotInit = true;
						if (jobBytes != null) {
							runJob(jobBytes, jobid);
							//DistributedJobResult result = runTask(jobParameter);
							//Message m_result = Message.create(Instruction.SEND_RESULT, jobid, serializeJobResult(result));
							//messageConnection.sendMessage(m_result);
							//jobParameter = null;
						}
						else {
							messageConnection.sendMessage(new Message(Instruction.ACK));

						}
						break;
					case PING:
						messageConnection.sendMessage(new Message(Instruction.PONG));
						break;
					case SEND_JOB:
						if (!gotInit) {

							jobBytes = (byte[]) m_in.getData(1); //= deserializeJobParameter((byte[]) m_in.getData(1), mClassLoader);
							//(DistributedJobParameter) m_in.getData(1);
							messageConnection.sendMessage(new Message(Instruction.REQUEST_INITIAL, m_in.getData(0)));
							break;
						}
						jobBytes = null;
						runJob((byte[]) m_in.getData(1), m_in.getData(0));
						//jobid = m_in.getData(0);
						//DistributedJobParameter parameters = deserializeJobParameter((byte[]) m_in.getData(1), mClassLoader);
						//(DistributedJobParameter) m_in.getData(1);
						//DistributedJobResult result = runTask(parameters);
						//Message m_result = Message.create(Instruction.SEND_RESULT, jobid, serializeJobResult(result));
						//messageConnection.sendMessage(m_result);
						break;
				}


			}

		}
		catch (InterruptedIOException iex) {
			LOGGER.log(Level.FINE, String.format("TestDroid %s: interrupted => stop", mID));
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		finally {
			LOGGER.log(Level.INFO, String.format("TestDroid %s: stop", mID));
		}


	}

	private void runJob(final byte[] parameterBytes, final Serializable jobID) {
		if (mJobThread != null) {
			return;
		}
		mJobThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					DistributedJobParameter parameter = deserializeJobParameter(parameterBytes, mClassLoader);
					DistributedJobResult result = task.runJob(parameter);
					messageConnection.sendMessage(new Message(Instruction.SEND_RESULT, jobID, serializeJobResult(result)));
				}
				catch (IOException ex) {
					Logger.getLogger(TestDroid.class.getName()).log(Level.SEVERE, null, ex);
				}
				mJobThread = null;
			}
		});
		try {
			messageConnection.sendMessage(new Message(Instruction.ACK));
		}
		catch (IOException ex) {
			Logger.getLogger(TestDroid.class.getName()).log(Level.SEVERE, null, ex);
		}
		mJobThread.start();
	}

	public InputStream getInputStream() {
		return ois;
	}

	public OutputStream getOutputStream() {
		return oos;
	}
}
