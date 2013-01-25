package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.MessageConnection;
import candis.common.fsm.FSM;
import candis.common.fsm.StateMachineException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
public class ClientConnection extends MessageConnection implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(ClientConnection.class.getName());
	protected final DroidManager mDroidManager;
	protected final JobDistributionIOServer mJobDistIO;
	protected FSM mStateMachine = null;
	private boolean isStopped;
	private String droidID;

	public ClientConnection(
					final Socket socket,
					final DroidManager droidmanager,
					final JobDistributionIOServer jobDistIO) throws IOException {
		super(socket, jobDistIO.getCDBLoader().getClassLoaderWrapper());
		mDroidManager = droidmanager;
		mJobDistIO = jobDistIO;
		mStateMachine = new ServerStateMachine(this, mDroidManager, mJobDistIO);
	}

	public ClientConnection(
					final InputStream in,
					final OutputStream out,
					final DroidManager droidmanager,
					final JobDistributionIOServer jobDistIO) throws IOException {
		super(in, out, jobDistIO.getCDBLoader().getClassLoaderWrapper());
		mDroidManager = droidmanager;
		mJobDistIO = jobDistIO;
		mStateMachine = new ServerStateMachine(this, mDroidManager, mJobDistIO);
	}

	public FSM getStateMachine() {
		return mStateMachine;
	}

	public void setDroidID(final String droidID) {
		this.droidID = droidID;
	}

	public String getDroidID() {
		return droidID;
	}

	@Override
	public void run() {

		// Init ping timer
		Timer pingTimer = new Timer();
		PingTimerTask ptt = new PingTimerTask();
		pingTimer.scheduleAtFixedRate(ptt, 3000, 3000);

		// init state machine
		mStateMachine.init();

		// Handle incoming client requests
		try {
			while ((!isStopped) && (!isSocketClosed())) {

				Message msg = readMessage();
				try {
					if (msg.getData() == null) {
						mStateMachine.process(msg.getRequest());
					}
					// IF PONG received, clear flag
					else if (msg.getRequest() == Instruction.PONG) {
						LOGGER.info("Got PONG reply, client is alive");
						ptt.clearFlag();
					}
					else {
						mStateMachine.process(msg.getRequest(), (Object[]) (msg.getData()));
					}
				}
				catch (StateMachineException ex) {
					Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
				}

				LOGGER.log(Level.INFO, "Client request: {0}", msg.getRequest());
			}
		}
		catch (InterruptedIOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
		catch (IOException ex) {
			LOGGER.log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Sends ping message.
	 */
	private class PingTimerTask extends TimerTask {

		private boolean mFlag;

		@Override
		public void run() {
			try {
				if (mFlag) {
					LOGGER.severe("Error: no ping reply!");
				}
				else {
					LOGGER.info("Sending PING message");
					sendMessage(Message.create(Instruction.PING));
					mFlag = true;
				}
			}
			catch (IOException ex) {
				Logger.getLogger(ClientConnection.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		public void clearFlag() {
			mFlag = false;
		}
	};
}
