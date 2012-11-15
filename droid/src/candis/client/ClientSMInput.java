package candis.client;

import candis.common.fsm.SMInput;

/**
 *
 * @author Enrico Joerns
 */
public enum ClientSMInput implements SMInput {

	//-- Input events for state machine
	SOCKET_CONNECTED,
	SERVER_MSG,
	JOB_FINISHED;
	//--
	
	private Object object;

	@Override
	public Object getData() {
		return object;
	}

	@Override
	public void setData(final Object obj) {
		object = obj;
	}
}
