package candis.client;

import candis.common.Instruction;
import candis.common.Message;
import candis.common.fsm.SMInput;
import candis.common.fsm.State;
import candis.common.fsm.StateMachineException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Enrico Joerns
 */
enum ClientStateMachine implements State {

	/**
	 * Client is not connected to the master.
	 */
	UNCONNECTED {
		@Override
		public State process(final SMInput input) {
			logger.log(Level.FINER, "Called process() in UNCONNECTED");
			State ret;
			if (((ClientSMInput) input) == ClientSMInput.SOCKET_CONNECTED) {
				ret = WAIT_ACCEPT;
				// send connection request
			} else {
				ret = UNCONNECTED;
			}
			return ret;
		}
	},
	/**
	 * Client is connected to the master server socket but waits for an accept
	 * from master.
	 */
	WAIT_ACCEPT {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			logger.log(Level.FINER, "Called process() in WAIT_ACCEPT");
			State ret;
			if (input == null) {
				throw new StateMachineException();
			}
			switch ((ClientSMInput) input) {
				case SERVER_MSG:
					break;
				case SOCKET_CONNECTED:
					break;
				default:
					break;
			}
			switch (((Message) input.getData()).getRequest()) {
				case REQUEST_PROFILE:
					ret = PROFILE_SENT;
					break;
				case ACCEPT_CONNECTION:
					ret = WAIT_FOR_JOB;
					break;
				default:
					ret = UNCONNECTED;
					break;
			}
			return ret;
		}
	},
	/**
	 * Client has requested connection to master and master has requested profile
	 * data.
	 *
	 * - ACCEPT_CONNECTION -> WAIT_FOR_JOB
	 */
	PROFILE_SENT {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			logger.log(Level.FINER, "Called process() in PROFILE_SENT");
			if (((ClientSMInput) input) != ClientSMInput.SERVER_MSG) {
				return UNCONNECTED;
			}
			if (((Message) ((ClientSMInput) input).getData()).getRequest()
							!= Instruction.ACCEPT_CONNECTION) {
				return UNCONNECTED;
			}
			return WAIT_FOR_JOB;
		}
	},
	/**
	 * Client is fully connected to the master and waits for a job.
	 */
	WAIT_FOR_JOB {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			logger.log(Level.FINER, "Called process() in WAIT_FOR_JOB");
			if (((ClientSMInput) input) != ClientSMInput.SERVER_MSG) {
				return UNCONNECTED;
			}
			if (((Message) ((ClientSMInput) input).getData()).getRequest()
							!= Instruction.SEND_JOB) {
				return UNCONNECTED;
			}
			return RECEIVED_JOB;
		}
	},
	/**
	 *
	 */
	RECEIVED_JOB {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			logger.log(Level.FINER, "Called process() in RECEIVED_JOB");
			return RECEIVED_JOB;
		}
	};
	private static final String TAG = "ClientStateMachine";
	private static final Logger logger = Logger.getLogger(TAG);
}
