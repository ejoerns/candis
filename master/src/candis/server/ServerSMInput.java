package candis.server;

import candis.common.fsm.SMInput;

/**
 *
 * @author Enrico Joerns
 */
public enum ServerSMInput implements SMInput {

	CLIENT_MSG,
	CLIENT_BLACKLISTED,
	NEW_CLIENT,
	CLIENT_ACCEPTED,
	POST_JOB;
				
	private Object data;

	@Override
	public Object getData() {
		return data;
	}

	@Override
	public void setData(final Object d) {
		data = d;
	}
	
}
