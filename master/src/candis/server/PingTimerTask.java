package candis.server;

import candis.common.Instruction;
import candis.common.Message;
import java.util.TimerTask;
import java.util.logging.Logger;

/**
 * Sends ping message.
 */
public class PingTimerTask extends TimerTask {

	private boolean mFlag;
	private final ClientConnection outer;
	private static final Logger LOGGER = Logger.getLogger(PingTimerTask.class.getName());

	PingTimerTask(final ClientConnection outer) {
		this.outer = outer;
	}

	@Override
	public void run() {
		// TODO:. start timer...
//		try {
		// if flag was not set, communication timed out, thus close socket.
		if (mFlag) {
			// TODO... reset timer
//				outer.getStateMachine().process(ServerStateMachine.ServerTrans.CLIENT_DISCONNECTED);
			cancel();
		}
		else {
			LOGGER.fine("Sending PING message");
			outer.sendMessage(Message.create(Instruction.PING));
			mFlag = true;
		}
		// TODO... handle timeout
	}
}
