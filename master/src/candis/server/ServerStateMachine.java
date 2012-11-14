package candis.server;

import candis.common.Instruction;
import candis.common.fsm.SMInput;
import candis.common.fsm.State;
import candis.common.fsm.StateMachineException;

/**
 *
 * @author Enrico Joerns
 */
public enum ServerStateMachine implements State {

	/**
	 * The droid is not connected to the master.
	 */
	UNCONNECTED {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			ServerSMInput ssmi = (ServerSMInput) input;
			switch (ssmi) {
				case CLIENT_MSG:
					switch ((Instruction) ssmi.getData()) {
						case REQUEST_CONNECTION:
							return CHECK;
						default:
							return UNCONNECTED;
					}
				default:
					return UNCONNECTED;
			}
		}
	},
	/**
	 * Droid requested connection and droid checks it.
	 */
	CHECK {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			ServerSMInput ssmi = (ServerSMInput) input;
			switch (ssmi) {
				case CLIENT_ACCEPTED:
					return CONNECTED;
				case CLIENT_BLACKLISTED:
					return UNCONNECTED;
				case NEW_CLIENT:
					return PROFILE_REQUEST;
				default:
					return CHECK;
			}
		}
	},
	/**
	 * Master requested profile from droid.
	 */
	PROFILE_REQUEST {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			ServerSMInput ssmi = (ServerSMInput) input;
			switch (ssmi) {
				case CLIENT_MSG:
					switch ((Instruction) ssmi.getData()) {
						case SEND_PROFILE:
							return CONNECTED;
						default:
							return PROFILE_REQUEST;
					}
				default:
					return PROFILE_REQUEST;
			}
		}
	},
	/**
	 * Droid is connected to master.
	 */
	CONNECTED {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			ServerSMInput ssmi = (ServerSMInput) input;
			switch (ssmi) {
				case POST_JOB:
					return JOB_SENT;
				default:
					return CONNECTED;
			}
		}
	},
	/**
	 * A job was sent to the connected droid.
	 */
	JOB_SENT {
		@Override
		public State process(final SMInput input) throws StateMachineException {
			ServerSMInput ssmi = (ServerSMInput) input;
			switch (ssmi) {
				case CLIENT_MSG:
					switch ((Instruction) ssmi.getData()) {
						case SEND_RESULT:
							return CONNECTED;
						default:
							return JOB_SENT;
					}
				default:
					return JOB_SENT;
			}
		}
	};
}
